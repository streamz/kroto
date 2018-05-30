# kroto
Klustering R0uter T0pology

Harry Kroto, was an English chemist. 
He discovered and named the buckminsterfullerene, which looks a bit like a routing topology!

![buckminsterfullerene](buckminsterfullerene.png)

## Overview
The kroto library provides a thin wrapper around the JGroups framework. 

kroto was designed to provide clustered JVM applications an API to dynamically find live service endpoints.
 
There are four core entities in kroto:
* Selector - 
* Mapper
* LoadBalancer
* Topology 

| Component  | Description |
| ---------- | ----------- |
| Selector   | Given a key, return a live endpoint |
| ReplicaSet     | 