import argparse
import os
import hashlib
import boto3
import botocore

thisset = set()


# Function to upload a file to S3
def upload_file(file_path, bucket_name, bucket_path, object_key):
    # Let's use Amazon S3
    s3 = boto3.client('s3')
    if bucket_path != '':
        if bucket_path[-1] == '/':
            object_key = bucket_path + object_key
        else:
            object_key = bucket_path + "/" + object_key

    s3.upload_file(file_path, bucket_name, object_key)
    print("File {} uploaded to S3".format(object_key))


# Function to download a file from S3
def download_file(bucket_name, object_key, file_path):
    s3 = boto3.client('s3')
    s3.download_file(bucket_name, object_key, file_path)
    print("File {} downloaded to {}".format(object_key, file_path))


# Function to recursively traverse a directory and upload all files to S3
def backup_directory(directory_path, bucket_path, bucket_name, location_constraint):
    s3 = boto3.client('s3')
    for root, dirs, files in os.walk(directory_path):
        for file in files:
            file_path = os.path.join(root, file)
            file_rel_path = os.path.relpath(file_path, directory_path)
            object_key = os.path.relpath(directory_path) + "/" + file_rel_path
            file_abs_path = os.path.abspath(directory_path) + "/" + file_rel_path
            # Calculate the MD5 hash of the local file
            md5 = hashlib.md5()
            with open(os.path.join(root, file), 'rb') as f:
                for chunk in iter(lambda: f.read(4096), b""):
                    md5.update(chunk)

            local_md5 = md5.hexdigest()

            cloud_key = os.path.join(bucket_path, root, file)

            # check if the file exists in s3
            if is_duplicate_file(bucket_name=bucket_name, bucket_dir=bucket_path, local_md5=local_md5,
                                 cloud_key=cloud_key, file=file, location_constraint=location_constraint):
                print("File {} already exists in S3".format(object_key))
            else:
                upload_file(file_path=file_abs_path, bucket_path=bucket_path, bucket_name=bucket_name,
                            object_key=object_key)


# Function to restore a directory from S3
def restore_directory(bucket_name, prefix, directory_path):
    s3 = boto3.resource('s3')
    bucket = s3.Bucket(bucket_name)
    for object in bucket.objects.filter(Prefix=prefix):
        print("object.key: ", object.key)
        if object.key[-1] == '/':
            # If the object is a directory, create it locally
            os.makedirs(os.path.join(directory_path, object.key[len(prefix):-1]), exist_ok=True)
        else:
            # If the object is a file, download it locally
            file_path = os.path.join(directory_path, object.key[len(prefix):])
            os.makedirs(os.path.dirname(file_path), exist_ok=True)
            download_file(bucket_name, object.key, file_path)


def is_duplicate_file(bucket_name, bucket_dir, local_md5, cloud_key, file, location_constraint):
    s3 = boto3.resource('s3')
    s_3 = boto3.client('s3')
    bucket = s3.Bucket(bucket_name)
    try:
        s_3.head_bucket(Bucket=bucket_name)
    except botocore.exceptions.ClientError as e:
        if e.response['Error']['Code'] == '404':

            s_3.create_bucket(Bucket=bucket_name, CreateBucketConfiguration={'LocationConstraint': location_constraint})
        else:
            print("unable to create bucket")
            return

    if not any(obj.key.startswith(bucket_dir) and obj.key.endswith('/') for obj in
               bucket.objects.filter(Prefix=bucket_dir)):
        # If the directory does not exist, create it in the bucket
        bucket.put_object(Key=bucket_dir, Body='')
        return False

    try:
        s3_object_metadata = s_3.head_object(Bucket=bucket_name, Key=cloud_key)
        e_tag = s3_object_metadata['ETag']

        if e_tag[1:-1] == local_md5 and e_tag not in thisset:

            thisset.add(e_tag)
            return True
        else:
            print(f'File {file} does not exist in the cloud')
            return False
    except botocore.exceptions.ClientError as e:
        if e.response['Error']['Code'] == '404':
            print(f'File {file} does not exist in the cloud')
            return False


# Parse command line arguments
parser = argparse.ArgumentParser(description='Backup or restore a directory to/from the cloud')
parser.add_argument('action', type=str, choices=['backup', 'restore', 'get'], help='Backup or restore action')
parser.add_argument('--cloud_path', type=str, help='Cloud path in the format bucket-name::directory-name',
                    required=False)
parser.add_argument('--local_dir', type=str, help='Local directory path', required=False)
parser.add_argument('--bucket_path', type=str, help='S3 bucket name')
parser.add_argument('--location_constraint', type=str, help='S3 location constraint')
args = parser.parse_args()

bucket_name = args.bucket_path.split('::')[0]
bucket_path = 'backup/'

if len(args.bucket_path.split('::')) > 1:
    bucket_path = args.bucket_path.split('::')[1]

if args.action == 'backup':
    # Backup directory to the cloud
    backup_directory(directory_path=args.local_dir, bucket_path=bucket_path, bucket_name=bucket_name, location_constraint=args.location_constraint)
if args.action == 'restore':
    # Restore directory from the cloud
    restore_directory(bucket_name, bucket_path + '/', args.local_dir)
