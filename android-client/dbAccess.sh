cd ~/AppData/Local/Android/sdk/platform-tools
DEVICE="$(./adb.exe devices | tail -n +2 | cut -f 1)"

./adb.exe -s "$DEVICE" root
./adb.exe -s "$DEVICE" shell 'sqlite3 data/data/ch.epfl.sweng.project/databases/polylove.db'
