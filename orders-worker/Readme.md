## What a shaded (“fat/uber”) JAR does?

- Unpacks all your runtime dependencies (Jackson, etc.) and repackages them inside one JAR together with your classes.
- Writes a proper Main-Class into the manifest so java -jar knows which class to boot.
- Optionally merges or transforms resource files (e.g., META-INF/services/\*) so service loaders continue to work.
- Optionally relocates packages (rename namespaces) to avoid dependency conflicts—handy when two libs embed different versions of the same class.

```bash
mvn -q -DskipTests package
java -jar target/orders-worker-1.0.0-shaded.jar           # read flow
java -jar target/orders-worker-1.0.0-shaded.jar write     # write flow
```
