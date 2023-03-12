import os

from django.shortcuts import render, redirect
from django.http import HttpResponse
from django.views.decorators.csrf import csrf_exempt
from django.contrib import messages
from .form import NameForm
from azure.storage.blob import BlobServiceClient, BlobClient
from azure.cosmos import CosmosClient
import hashlib
import dotenv

dotenv.read_dotenv()
ENDPOINT = os.environ.get('ENDPOINT')
KEY = os.environ.get('KEY')
DATABASE_NAME = os.environ.get('DATABASE_NAME')
CONTAINER_NAME = os.environ.get('CONTAINER_NAME')
SOURCE_BLOB_URL = os.environ.get('SOURCE_BLOB_URL')
DESTINATION_BLOB_CONNECTION_STRING = os.environ.get('DESTINATION_BLOB_CONNECTION_STRING')
def line_parser(line):
    data = {}
    parts = line.split()

    data['last_name'] = parts[0]
    data['first_name'] = parts[1]
    data['id'] = hashlib.md5((parts[0]+parts[1]).encode()).hexdigest()
    for part in parts[2:]:
        key, value = part.split('=')
        data[key] = value

    return data

def load_data():
    # Create a BlobClient object for the source blob using the URL link
    source_blob_client = BlobClient.from_blob_url(
        SOURCE_BLOB_URL)
    # Create a BlobServiceClient object for the destination blob storage account using the account key
    destination_blob_service_client = BlobServiceClient.from_connection_string(DESTINATION_BLOB_CONNECTION_STRING)

    # Get a BlobClient object for the destination blob
    destination_blob_client = destination_blob_service_client.get_blob_client(
        container='program-container', blob='input.txt')
    # Copy the source blob to the destination blob

    destination_blob_client.start_copy_from_url(source_blob_client.url)

    download_stream = destination_blob_client.download_blob(
        encoding='utf-8')

    lines = download_stream.readall().splitlines()

    parsed_data = []

    for line in lines:
        parsed_data.append(line_parser(line))

    client = CosmosClient(ENDPOINT, credential=KEY)
    database = client.get_database_client(DATABASE_NAME)
    container = database.get_container_client(CONTAINER_NAME)

    for item in parsed_data:
        container.upsert_item(body=item)

def clear_data():
    # delete all data in cosmos db
    client = CosmosClient(ENDPOINT, credential=KEY)
    database = client.get_database_client(DATABASE_NAME)
    container = database.get_container_client(CONTAINER_NAME)
    query = 'SELECT * FROM c'
    for item in container.query_items(query, enable_cross_partition_query=True):
        container.delete_item(item['id'], partition_key=item['id'])
    # delete the blob data
    # Create a BlobServiceClient object for the destination blob storage account using the account key
    destination_blob_service_client = BlobServiceClient.from_connection_string(
        DESTINATION_BLOB_CONNECTION_STRING)

    container_blob_client = destination_blob_service_client.get_container_client("program-container")
    blob_list = container_blob_client.list_blobs()
    container_blob_client.delete_blobs(*blob_list)
def message_pop(request):
    form = NameForm(request.POST)
    messages.success(request, form.data['first_name'] + ' ' + form.data['last_name'])

def query_data(request):
    form = NameForm(request.POST)
    client = CosmosClient(ENDPOINT, credential=KEY)
    database = client.get_database_client(DATABASE_NAME)
    container = database.get_container_client(CONTAINER_NAME)
    #query the data by first_name or last_name
    # Define the SQL query

    # Define the SQL query
    query = f"SELECT * FROM c WHERE c.first_name = '{form.data['first_name']}' OR c.last_name = '{form.data['last_name']}'"
    results = list(container.query_items(query=query, enable_cross_partition_query=True))
    # create a list of unknown key-value pairs
    unknown_items = []
    for doc in results:
        for key, value in doc.items():
            if key not in ['first_name', 'last_name']:
                unknown_items.append((key, value))
    # render the results in a template

    context = {
        'results': results,
        'unknown_items': unknown_items,
    }

    return render(request, 'hello_azure/index.html', context)

@csrf_exempt
def index(request):
    if request.method == 'POST':
        if 'load_btn' in request.POST:
            load_data()
            messages.success(request, "Loaded ")
            return redirect('index')
        elif 'clear_btn' in request.POST:
            clear_data()
            messages.success(request, "Cleared ")
            return redirect('index')
        elif 'query_btn' in request.POST:
            form = NameForm(request.POST)
            client = CosmosClient(ENDPOINT, credential=KEY)
            database = client.get_database_client(DATABASE_NAME)
            container = database.get_container_client(CONTAINER_NAME)
            #query the data by first_name or last_name
            # Define the SQL query

            # Define the SQL query
            query = f"SELECT * FROM c WHERE c.first_name = '{form.data['first_name']}' OR c.last_name = '{form.data['last_name']}'"
            results = list(container.query_items(query=query, enable_cross_partition_query=True))
            # create a list of unknown key-value pairs
            unknown_items = []
            for doc in results:
                for key, value in doc.items():
                    if key not in ['first_name', 'last_name']:
                        unknown_items.append((key, value))
            # render the results in a template

            context = {
                'results': results,
                'unknown_items': unknown_items,
            }
            messages.success(request, "Query results ")
            return render(request, 'hello_azure/index.html', context)



    else:
        return render(request, 'hello_azure/index.html')
