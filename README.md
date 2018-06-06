# kroto
Klustered R0uting T0pology

Harry Kroto, was an English chemist. 
He discovered and named the buckminsterfullerene, which looks a bit like a network topology!

![buckminsterfullerene](buckminsterfullerene.png)

## Overview
The kroto library provides a thin wrapper around the [JGroups reliable messaging toolkit. ](http://www.jgroups.org/)

kroto was designed to provide clustered JVM applications an API to dynamically find live service endpoints given a routing key.
Endpoints are grouped in ReplicaSets which make up a Topology.
The Topology is accessed by the application via a Selector when given a key, selects a ReplicaSet that returns a live endpoint via a LoadBalancer.

![selector](selector.png)
 
There are five core entities in kroto:

| Component  | Description |
| ---------- | ----------- |
| Selector   | Given a key, return a live endpoint. Selector is a thin wrapper around a Group and a Topology. |
| ReplicaSet | A virtual entity that is a mapping of a key to ReplicaSetId which in turn is a logical grouping of endpoints.|
| Mapper     | Employs a algorithms to access keys in a ReplicaSet. Available strategies are "mod", "ring" and "map". |
| LoadBalancer| A function that given a list of endpoints, selects a single endpoint based on a user provided algorithm. |
| Topology | A Topology connects a Mapper and a LoadBalancer to provide a Selector the ability given a key, to return a "live" endpoint in a cluster. |

## Example

Running  the clustered telnet example.
In this example we will start 4 telnet servers. 
There will be two replica sets with two servers each. 
Servers running on ports 8000 and 8001 will be part of replica set r8000.
Servers running on ports 9000 and 9001 will be part of replica set r9000.

In a terminal:
sbt assembly

cd main/bin

Start the 1st telnet server:

```bash
./main.sh \
-group=test \
-uri="tcp://localhost:8000?node=localhost:8000&node=localhost:8001&node=localhost:9000&node=localhost:9001" \
-service=http://localhost:8081 \
-telnetPort=8800 \
-replicaSets=r8000,r9000  
```
Start the 2nd telnet server

```bash
./main.sh \
-group=test \
-uri="tcp://localhost:8000?node=localhost:8000&node=localhost:8001&node=localhost:9000&node=localhost:9001" \
-service=http://localhost:8082 \
-telnetPort=8801 \
-replicaSets=r8000,r9000  
```

Start the 3rd telnet server

```bash
./main.sh \
-group=test \
-uri="tcp://localhost:9000?node=localhost:8000&node=localhost:8001&node=localhost:9000&node=localhost:9001" \
-service=http://localhost:9081 \
-telnetPort=9800 \
-replicaSets=r9000,r8000 
```

Start the 4th telnet server

```bash
./main.sh \
-group=test \
-uri="tcp://localhost:9001?node=localhost:8000&node=localhost:8001&node=localhost:9000&node=localhost:9001" \
-service=http://localhost:9082 \
-telnetPort=9801 \
-replicaSets=r9000,r8000 
```

Open up a shell and type:
```bash
$telnet 0 8800

Trying 0.0.0.0...
Connected to 0.
Escape character is '^]'.
    _...._
  .'   \ _'.
 /##\__/##\_\      __             __     
|\##/  \##/  |    / /__ _______  / /____ 
|/  \__/  \ _|   /  '_// __/ _ \/ __/ _ \
 \ _/##\__/#/   /_/\_\/_/  \___/\__/\___/
  '.\##/__.'          selector topology test app
    `""""`
kroto shell
Usage: kroto> [help] [leader] [top] [map <k=v>] [select <key>]
  select <key>  show the selected endpoint for key
  map <k=v k=v> sets the key mapping for the the topology
  smap          show the topology mapper
  top           show the kroto routing topology
  leader        show the cluster leader (denoted by *)
  help          show help
kroto>

```



