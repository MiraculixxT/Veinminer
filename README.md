## ⛏️ Veinminer
Mine a single block to mine the whole vein!
Highly configurable and works everywhere, even on your vanilla server.
Make the tedious mining experience to something satisfying and fun!<br>
Veinminer works server side, so all clients are supported. Even cross loaders & versions for addons!

## 🔻 Download
- [**Veinminer Mod/Plugin**](https://modrinth.com/project/veinminer) [(R)](./veinminer/README.md) - Full customizable mod & plugin for Fabric/Quilt, Paper/PurPur/Folia & NeoForge servers
- [**Veinminer Client Addon**](https://modrinth.com/project/veinminer-client) [(R)](./veinminer-client/README.md) - Only veinmine when pressing a hotkey & get a mining preview
- [**Veinminer Enchantment Addon**](https://modrinth.com/project/veinminer-enchantment) [(R)](./veinminer-enchant/README.md) - Add a veinmine enchantment to limit veinmining to only enchanted tools

## ️🧩 Preview
![](https://cdn-raw.modrinth.com/data/OhduvhIc/images/f4c0ad7fa3b8b579753c1f757e80151798717c68.gif)
If you need any help or want to share some ideas to add, just hop on our Discord ([dc.mutils.net](https://dc.mutils.net))

## 🛠️ Development

Task shortcuts for easier testing.

### Run Servers
```shell
./gradlew :veinminer:veinminer-paper:runServer
```
```shell
./gradlew :veinminer:veinminer-fabric:runServer
```
```shell
./gradlew :veinminer:veinminer-neoforge:runServer
```

### Run Clients
```shell
./gradlew :veinminer-client:veinminer-client-fabric:runClient
```
```shell
./gradlew :veinminer-client:veinminer-client-neoforge:runClient
```

### Building (bulked or project seperated)
```shell
./gradlew :veinminer:veinminer-paper:build :veinminer:veinminer-fabric:build :veinminer:veinminer-neoforge:build
./gradlew :veinminer-client:veinminer-client-fabric:build :veinminer-client:veinminer-client-neoforge:build
./gradlew :veinminer-enchant:build
```
