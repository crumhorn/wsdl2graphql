# WSDL to GraphQL Generator

This is a quick and hastily written project that takes a .wsdl file or a url to a wsdl and generates GraphQL schema from it, as best as it can. The result are by no means complete and may require manual reordering as the GraphQL schema has to be in order, but it will be a great start rather than writing the entire schema by hand.

Easiest is to import the project in IntelliJ IDEA and run it from there as then you don't have to worry about classpaths etc, however, the following should work as well:

## Changelog
- Sep 27, 2016 
    - First check in
- Feb 22, 2018
    - Fixed missing pom dependency, thanks ```@treksler``` 
    - Checked in code that allows for typeSchema generation rather than the raw JS (experimental) 
 
## Requirements

- Java 8+

## Important

The `Parser.java` class does a few assumptions that you may need to change to fit your WSDL/Service implementation, as this was written to target a specific Web Service.
 
- First, any WSDL method/function ending with "Response" is treated as a response call. See line 233.
- Second, any object name ending in "Enum" is treated as an Enum. See line 372.
 
## Compiling 

run maven `mvn clean install`
 
## Running
 
run `java Main` as follows:

_(You may need to inclue all the jars in the `./lib` classpath as part of the java command argument, as these are required)_

Normal:
- ```java Main -i wsdlUrlOrFile -o outputFile.graphql```

Output to Type Schema:
- ```java Main -i wsdlUrlOrFile -o outputFile.graphql -t typeSchema```
 
Option Details:
- `-i <inputFile>` is the input file or URL
- `-o <outputFile>` is the output file of the generated schema.
- `-t <schemaType>` optionally you can set this to ```typeSchema``` to output GraphQL type schema rather than normal JS
 
 _The IntelliJ IDEA project files are also included, so it may be even easier to run it from there if you have it, just add the launch parameters to the program arguments in the launcher_ 

## Modifying the Output

If you need to modify the template used to generate the output, see `resources/templates/schema.pebble`

## Adding the missing methods

As you will see in the output of the generated schema file, there are calls made to: 

`db.querySOAP` and `db.querySOAPWithArgs(...)` which will require your own implementation to call the SOAP backend. For our implementation we used the following NodeJS module:

https://www.npmjs.com/package/soap

## Notes

_The output is usually not very "pretty", so I suggest opening the file in your favorite editor and run a javascript formatter on it._

Enjoy!

Emil Crumhorn