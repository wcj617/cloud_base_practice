#!/bin/sh

# -- This shell script compiles the program with json and javc 
# -- After compilation, the program runs with the following command using argument

# You can run the program manually with your own city, as follows: 
# java -cp gson-2.8.9.jar : Api.java 

# Make sure any city with more than 1 word name surrounded by qutoes e.g., 

# This shell is for LINUX!
# For windows, instead of a colon after the java run command, change to ";."


javac -cp json-20220924.jar Api.java

java -cp json-20220924.jar: Api 