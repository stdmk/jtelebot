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

/ping — bot health check.

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

/uuid — random uuid generation.

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

### Properties
**telegramBotApiToken**  
You must specify the bot api token in the file properties.properties.  
To receive a token contact @BotFather.

**adminId**  
Telegram-admin user ID to gain access to all commands.  
To get the value of your ID, run the bot and use the /getid command.  
Then the admin must send the /help command to the bot for getting admin rights.

**openweathermapId**  
OpenWeatherMap API access token.  
To get a token go to https://openweathermap.org/

**googleToken**  
Google search API access token.  
To get a token go to https://developers.google.com/custom-search/v1/overview?hl=en#prerequisites

**googleTranslateToken**  
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

**screenshotMachineToken**  
Screenshotmachine API access token.  
To get a token go to https://www.screenshotmachine.com/

**wolframAlphaToken**  
WolframAlphaToken API access token.
To get a token go to https://products.wolframalpha.com/api

**kinopoiskToken**  
Kinopoisk.dev API access token.
To get a token go to @kinopoiskdev_bot.

**chatGptApiUrl**  
API URL of ChatGPT.  
If the value is empty, the official one will be used: https://api.openai.com/v1/

**chatGptToken**  
ChatGPT API access token.
To get a token go to https://openai.com/product (third party services are also suitable)

**spyMode**  
The bot will notify you by adminId when receiving personal messages if value `true`.

**russianPostLogin** and **russianPostPassword**  
Russian Post tracking access token.  
To get a credentials go to https://tracking.pochta.ru/support/faq/how_to_get_access

**russianPostRequestsLimit**  
Limit of API requests. 100 is the free account limit.

**chatGPTContextSize**  
Size of communication history with chatgpt, messages.  
16 by default. Reduce if problems arise.

**defaultLanguage**  
Default language code for the bot. Currently supported: `en`, `ru`. Default `en`.

**xmlTvFileUrl**  
URL to a TV program file in XMLTV (gz) format. For /tv command.

**saluteSpeechSecret**  
Secret for access for Salute Speech services (synthesis and recognize speech).  
To get a secret go to: https://developers.sber.ru/docs/ru/salutespeech/integration

**gigaChatSecret**  
Gigachat API access secret.
To get a secret go to: https://developers.sber.ru/studio/workspaces/my-space/get/gigachat-api

### System
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