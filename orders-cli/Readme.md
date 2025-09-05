```bash
mvn -q -DskipTests package
# READ flow (requests read:orders)
java -jar target/orders-cli-1.0.0-shaded.jar

# WRITE flow (also requests write:orders; user must have that permission)
java -jar target/orders-cli-1.0.0-shaded.jar write
```
