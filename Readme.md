# jtelebot
Java Telegram Bot (Spring)

### List of commands
/echo — chatbot (need training).

/level — command access level. From 0 (complete ignore) to 10 - full access.

/boobs — erotic pictures.

/butts — erotic pictures.

/help — bot commands documentation.

/getid — information on user and group IDs.

/start — welcome message from the bot with initial information. (Also, obtaining administrator rights for the user id specified in the properties).

/top — user statistics: the number of messages, images, karma etc. per day, month, all the time.

/sql — SQL command line with access to the bot database (use with caution, make backups (/backup)).

/bash — bash.org.ru site quotes.

/todo — todo-list.

/news — news from rss feeds.

/logs — bot logs.

/where — information about the last message of the specified user.

/cmd — access to the system console of the machine where the bot is running (use with caution).

/set — various bot settings: news, city, aliases, tv, holidays, disable commands, talker, zodiac sign, trainings, chatgpt.

/weather — weather information.

/password — password generation.

/google — search in google.

/picture — image search.

/ping — resources reachability.

/webscreen — screenshots of web pages.

/advice — random advices.

/truth — truthfulness of expressions.

/wiki — search on wikipedia.

/newyear — remaining time until the new year.

/turn — translation of text from one layout to another (ghbdtn -> привет).

/wolframalpha — access to Wolfram alpha system.

/alias — set aliases of bot commands.

/image — search or download images via direct links and send them to chat.

/karma — information and changing the karma of chat participants.

/time — current time of users (requires city setting).

/exchange — exchange rates.

/tv — TV programm.

/calc — calculator.

/uptime — bot statistics.

/shutdown — shutdown bot.

/holidays — holiday information.

/say — bot repeats what is written.

/up — shift up list of chat messages.

/speller — search for errors in the text.

/files — cloud storage.

/backup — bot database backup (sent as a file to telegram).

/cats — images of cats.

/timedelta — date time delta.

/translate — translator.

/qr — reading and generating qr-codes.

/timedownloading — download time calculation.

/horoscope — horoscope.

/webcam — webcam video.

/parcel — parcel tracking.

/remind — reminder.

/uuid — random uuid generation or checking.

/download — download files via direct links and send them to chat

/number — telephone number information.

/training — accounting of trainings, subscriptions to them and statistics.

/movie — movie information.

/calendar — public holiday calendar.

/errors — list of errors that have occurred in the bot (for administrators).

/chatgpt — chat with ChatGPT.

/convert — unit conversion.

/metadata — metadata from file.

/location — sending location by coordinates.

/dogs — images of dogs.

/gigachat — chat with Gigachat.

/voice — speech synthesis (and recognizing).

/virus — checking files and links for malware.

/length — counting the number of characters in a text or text file

/word — access to Wiktionary.

/random - get random in range or in list

/latin - search for non-Latin characters

/me - third person text (skype or irc /me command analog) (needs admin rights)

/delete - deleting messages

/delay - delayed execution of commands

/json - json validation and formatting

/xml - xml validation and formatting

### Properties
**telegramBotApiToken**  
You must specify the bot api token in the file properties.properties.  
To receive a token contact @BotFather.

**adminId**  
Telegram-admin user ID to gain access to all commands.  
To get the value of your ID, run the bot and use the /getid command.  
Then the admin must send the /help command to the bot for getting admin rights.

**openweathermapId** (/weather command)  
OpenWeatherMap API access token.  
To get a token go to https://openweathermap.org/

**googleToken** (/google /image /images commands)  
Google search API access token.  
To get a token go to https://developers.google.com/custom-search/v1/overview?hl=en#prerequisites

**googleTranslateToken** (/translate command)  
Google Translator API access token.  
To get a token: write and publish the script on https://script.google.com/ and copy value after https://script.google.com/macros/s/  
```
var mock = {
  parameter:{
    q:'hello',
    source:'en',
    target:'ru'
  }
};


function doGet(e) {
  e = e || mock;

  var sourceText = ''
  if (e.parameter.q){
    sourceText = e.parameter.q;
  }

  var sourceLang = '';
  if (e.parameter.source){
    sourceLang = e.parameter.source;
  }

  var targetLang = 'en';
  if (e.parameter.target){
    targetLang = e.parameter.target;
  }

  var translatedText = LanguageApp.translate(sourceText, sourceLang, targetLang, {contentType: 'html'});

  return ContentService.createTextOutput('{"text":"' + translatedText + '"}').setMimeType(ContentService.MimeType.JSON);
}
```

**screenshotMachineToken** (/screen command)  
Screenshotmachine API access token.  
To get a token go to https://www.screenshotmachine.com/

**wolframAlphaToken** (/wolframalpha command)  
WolframAlphaToken API access token.
To get a token go to https://products.wolframalpha.com/api

**kinopoiskToken** (/kinopoisk command)  
Kinopoisk.dev API access token.
To get a token go to @kinopoiskdev_bot.

**chatGptApiUrl** (/chatgpt command)  
API URL of ChatGPT.  
If the value is empty, the official one will be used: https://api.openai.com/v1/

**chatGptToken** (/chatgpt command)  
ChatGPT API access token.
To get a token go to https://openai.com/product (third party services are also suitable)

**spyMode**  
The bot will notify you by adminId when receiving personal messages if value `true`.

**russianPostLogin** and **russianPostPassword** (/parcel command)  
Russian Post tracking access token.  
To get a credentials go to https://tracking.pochta.ru/support/faq/how_to_get_access

**russianPostRequestsLimit** (/parcel command)  
Limit of API requests. 100 is the free account limit.

**chatGPTContextSize** (/chatgpt command)  
Size of communication history with chatgpt, messages.  
16 by default. Reduce if problems arise.

**defaultLanguage**  
Default language code for the bot. Currently supported: `en`, `ru`. Default `en`.

**xmlTvFileUrl** (/tv command)  
URL to a TV program file in XMLTV (gz) format. For /tv command.

**sberApiRequestTimeoutSeconds** (/voice and /gigachat commands)
Waiting time for response from sber API.  

**saluteSpeechSecret** (/voice command)  
Secret for access for Salute Speech services (synthesis and recognize speech).  
To get a secret go to: https://developers.sber.ru/docs/ru/salutespeech/integration

**gigaChatSecret** (/gigachat command)  
Gigachat API access secret.
To get a secret go to: https://developers.sber.ru/studio/workspaces/my-space/get/gigachat-api

**virusTotalApiKey** (/virus command)  
Virus Total API key  
To get a key go to: https://www.virustotal.com/

**ftpBackupUrl** (ftp backup)
Url of the ftp server where backups will be uploaded
`ftp://username:password@host:port[/path]`

### System
Java 17 is required to run the bot  
To use the /webcam command, you need to install ffmpeg on your system

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