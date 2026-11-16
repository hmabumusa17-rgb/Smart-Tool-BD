# SmartToolBD — ওয়েবসাইট, Medicine API ও Android APK

এই প্যাকেজে আছে:

1. `index.html` — সম্পূর্ণ ওয়েবসাইট; Medicine Finder ও Patient Safety Assistant সহ।
2. `config.js` — প্রকাশ্য কনফিগ; এখানে কোনো গোপন API Key রাখা যাবে না।
3. `sw.js` — ওয়েবসাইটের offline cache/service worker।
4. `backend/worker.js` ও `backend/wrangler.toml` — Cloudflare Worker API gateway-এর কোড।
5. `android/` — Kotlin Android WebView অ্যাপের সোর্স প্রজেক্ট।
6. `.github/workflows/android-apk.yml` — GitHub Actions দিয়ে APK build workflow।

## A. GitHub Pages-এ ওয়েবসাইট আপডেট

তোমার repository `Smart-Tool-BD`-এর root folder-এ:

- বর্তমান `index.html`-এর backup রাখো; তারপর এই প্যাকেজের `index.html` দিয়ে replace করো।
- `config.js` root-এ upload করো।
- `sw.js` root-এ replace করো।
- `backend/`, `android/`, `.github/workflows/` ফোল্ডারগুলো একই path-এ upload করো।
- অন্য পুরোনো asset বা ফাইল মুছবে না।

GitHub Pages URL: `https://hmabumusa17-rgb.github.io/Smart-Tool-BD/`

## B. Medicine API চালু করা — এখানে তোমার একবার সেটআপ লাগবে

এই Worker একটি gateway/template; এটি নিজে বাংলাদেশের medicine database নয়। বাস্তব ফলাফল পেতে তোমার অনুমোদিত/লাইসেন্সকৃত medicine data provider-এর API endpoint ও token লাগবে। Provider-এর API না থাকলে এই অংশ 503 দেখাবে, আর ওয়েবসাইটের external reference search চালু থাকবে।

Cloudflare Dashboard পদ্ধতি:

1. Cloudflare-এ account তৈরি/লগইন করো → Workers & Pages → Create → Worker।
2. Worker code editor-এ `backend/worker.js`-এর সম্পূর্ণ কোড paste করে Deploy করো।
3. Worker Settings → Variables and Secrets-এ `ALLOWED_ORIGIN` নামে plain variable দাও: `https://hmabumusa17-rgb.github.io`.
4. `MEDICINE_API_URL` এবং `MEDICINE_API_TOKEN`-কে Secret হিসেবে যোগ করো। Provider-এর কাছ থেকে এই তথ্য নিতে হবে। Token কখনো `index.html`, `config.js` বা public GitHub file-এ লিখবে না।
5. Deploy হওয়া Worker URL কপি করো।
6. `config.js`-এ `medicineApiBase`-এর খালি string-এর জায়গায় Worker URL বসাও। যেমন: `https://your-worker.your-subdomain.workers.dev`.
7. `config.js` GitHub-এ update করো।

Provider-এর response-এ `items`, `results`, বা `data` array থাকা উচিত। প্রতিটি item-এ brand/name, generic, company/manufacturer, group/class, uses/indications, strength, form, sourceUrl ফিল্ড থাকতে পারে। Provider-এর schema আলাদা হলে Worker mapping বদলাতে হবে।

**গুরুত্বপূর্ণ:** এই Worker কোনো চিকিৎসা-পরামর্শ বা ডোজ তৈরি করে না। শুধু provider-এর supplied data দেখায়। Provider-এর license/terms মেনে চলবে।

## C. APK build

GitHub-এ ফাইলগুলো upload করার পর:

1. Repository → Actions tab খোলো।
2. `Build SmartToolBD APK` workflow নির্বাচন করো।
3. `Run workflow` চাপো।
4. Build শেষ হলে workflow run-এর নিচে Artifacts থেকে `SmartToolBD-debug-APK` ডাউনলোড করো।
5. ZIP extract করে `app-debug.apk` Android ফোনে install করো। Android Settings-এ browser/file manager-এর জন্য “Install unknown apps” অনুমতি দিতে হতে পারে।

Workflow-এ Android Project-এর পরিবর্তন push হলেও build trigger হবে।

## D. Signed release APK (ঐচ্ছিক, পরবর্তী ধাপ)

Workflow ডিফল্টে debug APK তৈরি করবে। স্থায়ীভাবে update install করা ও Play Store-এর জন্য নিজের release keystore প্রয়োজন। Keystore নিজে তৈরি করে নিরাপদে backup রাখবে; হারালে ভবিষ্যৎ update-এ সমস্যা হবে। GitHub repository → Settings → Secrets and variables → Actions-এ নিচের চারটি Secret যোগ করতে হবে:

- `ANDROID_KEYSTORE_BASE64` — keystore file-এর Base64 text
- `ANDROID_KEY_ALIAS` — key alias
- `ANDROID_KEY_PASSWORD` — key password
- `ANDROID_STORE_PASSWORD` — keystore password

এই চারটি Secret থাকলে workflow signed release APK-ও build করবে। Keystore কখনো public repository-তে upload করবে না।

## E. সীমাবদ্ধতা / পরীক্ষা

- HTML, OCR ও voice input browser/device support ও internet-এর ওপর নির্ভর করে।
- OCR ভুল পড়তে পারে; ওষুধের নাম, strength ও form প্যাকেটের সঙ্গে মিলিয়ে নিতে হবে।
- বাংলাদেশের পূর্ণ ওষুধের তালিকা এই প্যাকেজে অন্তর্ভুক্ত নয়; licensed provider বা যাচাইকৃত ডেটাসেট আলাদাভাবে যুক্ত করতে হবে।
- Android source তৈরি আছে; GitHub Actions চালানোর আগে এই environment-এ APK compile করে যাচাই করা হয়নি।
- Android app বর্তমানে তোমার live GitHub Pages URL লোড করে। URL বদলালে `MainActivity.kt`-এর `startUrl` ও `siteHost` বদলাবে।
