# jtelebot
Java Telegram Bot (Spring)

### Properties
You must specify the bot api token in the file properties.properties

### Build
To build jar-file:

`gradlew build`

### Run
To run without jar-file:

`gradlew bootrun`

To run with jar file:

put properties.properties in folder with jar file and

`java -jar Bot.jar`

Use this for reduce memory usage

`java -Xms132m -Xmx264m -jar Bot.jar -XX:+UseSerialGC -Xss512k -XX:MaxRAM=72m`