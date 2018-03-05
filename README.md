# WSDL to GraphQL Generator

This is a quick and hastily written project that takes a .wsdl file or a url to a wsdl and generates GraphQL schema from it, as best as it can. The result are by no means complete and may require manual reordering as the GraphQL schema has to be in order, but it will be a great start rather than writing the entire schema by hand.

Easiest is to import the project in IntelliJ IDEA and run it from there as then you don't have to worry about classpaths etc, however, the following should work as well:

## Changelog
- See [CHANGELOG.md](CHANGELOG.md)
 
## Requirements

- Java 8+

## Important

The `Parser.java` class does a few assumptions that you may need to change to fit your WSDL/Service implementation, as this was written to target a specific Web Service.
 
- First, any WSDL method/function ending with "Response" is treated as a response call. See line ```279```.
- Second, any object name ending in "Enum" is treated as an Enum. See line ```469```.
 
## Compiling 

run maven `mvn clean install`
 
## Running
 
run `java Main` as follows:

_(You may need to inclue all the jars in the `./lib` classpath as part of the java command argument, as these are required)_

Normal:
- ```java Main -i wsdlUrlOrFile -o outputFile.graphql```

Output to [Type Schema](http://graphql.org/learn/schema/):
- ```java Main -i wsdlUrlOrFile -o outputFile.graphql -t typeSchema```
 
Option Details:
- `-i <inputFile>` is the input file or URL
- `-o <outputFile>` is the output file of the generated schema.
- `-t <schemaType>` optionally you can set this to ```typeSchema``` to output GraphQL type schema rather than normal JS
 
 _The IntelliJ IDEA project files are also included, so it may be even easier to run it from there if you have it, just add the launch parameters to the program arguments in the launcher_ 

## Modifying the Output

If you need to modify the template used to generate the output, see `resources/templates/schema.pebble`

For modifying the Type Schema output, change the `resources/templates/typeSchema.pebble` file.

## Adding the missing methods

As you will see in the output of the generated schema file, there are calls made to: 

`db.querySOAP(...)` and `db.querySOAPWithArgs(...)` which will require your own implementation to call the SOAP backend. For our implementation we used the following NodeJS module:

If you are generating Type Schema then the calls will be named `db.soapMethodName();`

https://www.npmjs.com/package/soap

## Notes

_The output is usually not very "pretty", so I suggest opening the file in your favorite editor and run a javascript formatter on it._

## GraphQL queries to SOAP conversion in NodeJS

Just in case someone gets stuck on this, this is the JavaScript (on the server-side (NodeJS in my case)) that I wrote to pass in a GraphQL query "Json" to convert it over to a valid SOAP WSDL request, as SOAP needs multi-value arrays to be repeated many times. I believe the documentation of it should speak for itself.

```javascript
/**
 * This method fixes the passed in GraphQL parameters and turns them into a correct JSON formatted object so that the soap body is correct. Thus, a query like this:
 *
 * findByCriteria(arg0: "{currencyCodes:[CHF,USD],maxResults:1500}") {...}
 *
 * example (copy and paste into GraphiQL):
 *    findByCriteria(arg0:"{doAddPublicationData:true,doAddStaticData:true,fromDate:\"2017-01-11T00:00:00+01:00\",maxResults:1500}") {
 *
 * Is turned into
 *
 * arg0 {
 *      // all booleans etc are standard per the query, it's the array that's crucial
 *      "currencyCodes":[CHF,USD]
 *      "maxResults":1500
 * }
 *
 * Which in turn creates the correct SOAP Body:
 * <soap:Body>
 *     <tns:findByCriteria xmlns:tns="http://xxx.yyy.com/">
 *         <arg0>
 *             <currencyCodes>CHF</currencyCodes>
 *             <currencyCodes>USD</currencyCodes>
 *             <maxResults>1500</maxResults>
 *         </arg0>
 *     </tns:findByCriteria>
 * </soap:Body>
 *
 * Do note that stuff like setXXX(arg0:590,arg1:"OK") must also work here and be translated into nothing unless the key has {} inside the text
 *
 * @param params arguments
 * @returns {{}}
 */
function fixParams(params) {
    try {
        // put quotes around keys regex, but not for commas inside quotes, see here: https://stackoverflow.com/questions/21105360/regex-find-comma-not-inside-quotes
        // test it here: https://regex101.com/r/FcZVke/8
        let objKeysRegex = /({|(?!\B"[^"]*),(?![^"]*"\B))(?:\s*)(?:')?([A-Za-z_$\.][A-Za-z0-9_ \-\.$]*)(?:')?(?:\s*):/g;// look for object names
        let json = {};

        for (let prop in params) {
            let val = '' + params[prop];
            if (!val.startsWith('{') && !val.startsWith('[')) {              
                json[prop] = params[prop];
                continue;
            }

            // need to stringify in case it's a number or such
            let text = val.replace(objKeysRegex, "$1\"$2\":");
            // put the json property of the same name to the parsed JSON object
            json[prop] = JSON.parse(text);

        }

        // debug(json);
        return json;
    }
    catch (err) {
        console.error(err);
        return params;
    }
}
```

Enjoy!

Emil Crumhorn