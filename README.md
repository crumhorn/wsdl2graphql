# WSDL to GraphQL Generator

This is a quick and hastily written project that takes a .wsdl file or a url to a wsdl and generates GraphQL schema from it, as best as it can. The result are by no means complete and may require manual reordering as the GraphQL schema has to be in order, but it will be a great start rather than writing the entire schema by hand.

Easiest is to import the project in IntelliJ IDEA and run it from there as then you don't have to worry about classpaths etc, however, the following should work as well:
 
## Requirements

- Java 8
 
## Compiling 

run maven `mvn clean install`
 
## Running
 
run `java Main` as follows:

_(You may need to inclue all the jars in the `./lib` classpath as part of the java command argument, as these are required)_

```java Main -i wsdlUrlOrFile -o outputFile.graphql```
 
`-i <inputFile>` is the input file or URL
`-o <outputFile>` is the output file of the generated schema.
 
 _The IntelliJ IDEA project files are also included, so it may be even easier to run it from there if you have it, just add the launch parameters to the program arguments in the launcher_ 

## Modifying the Output

If you need to modify the template used to generate the output, see `resources/templates/schema.pebble`

## Notes

_The output is usually not very "pretty", so I suggest opening the file in your favorite editor and run a javascript formatter on it._

Enjoy!

Emil Crumhorn