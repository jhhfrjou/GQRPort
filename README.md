# GQR

GQR rewrites queries with only the source-to-target-tgds and a query. GQR was originally written to read and write in Datalog formats. I have ported this to use chasebench parsing and classes so that it can be easily used with chasebench tools and the obdabenchmark. This version of GQR takes in a file with one or many queries in chasebench format and will return an SQL query. With multiple queries in a file this SQL query will be the union of all the queries in the file.

```
java -jar GQR.jar -st-tgds <source to target tgds> -q <rule query file>
```