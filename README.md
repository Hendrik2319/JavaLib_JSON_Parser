# JavaLib_JSON_Parser
This is a library to support parsing JSON files.  
The main concern is the parsing of the JSON structure after the parsing of the JSON code.
The main use case of this library is to parse JSON files that are not written by the developer.
So developer have to expect, that the structure of the JSON data can change over time.
He can use "tolerant" methods for parsing or exception throwing methods, that detect every unexpected change in the structure of the JSON data.  
Developer can add additional data or functionalities to each node (-> `JSON_Data`) in the parsed JSON structure.
The ability to mark "processed nodes" is already provided.

`JSON_Parser` is the parser for the JSON code, that build the JSON structure.

`JSON_Data` contains the classes for the nodes that make up the JSON structure build by the parser.

`JSON_Helper` contains 3 classes to help to parse unknown or unfixed JSON structures.
* `KnownJsonValues` and `KnownJsonValuesFactory` notifies, when a JSON object contains new fields or fields with unexpected types.
* `OptionalValues` can scan a whole unknown JSON structure and reports found field names and value types.
It is especially useful when a repeated use of nearly same structures indicates an implementation of a parse method for these structures.

### Usage / Development
The current state of this library is compatible with JAVA 17. But most of its code should be compatible with JAVA 8.  
This is an Eclipse project. `.classpath` and `.project` files are contained in the repository.  
`JavaLib_JSON_Parser` don't depends on other libraries.
