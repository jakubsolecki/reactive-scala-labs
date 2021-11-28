#!/usr/bin/env bash

sbt "runMain EShop.lab6.cluster.SeedNode seed-node1"
sbt "runMain EShop.lab6.cluster.SeedNode seed-node2"
sbt "runMain EShop.lab6.cluster.SeedNode"

sbt "runMain EShop.lab6.cluster.ClusterProductCatalogServerApp 9001"
sbt "runMain EShop.lab6.cluster.ClusterProductCatalogServerApp 9002"
sbt "runMain EShop.lab6.cluster.ClusterProductCatalogServerApp 9003"


# start gatling tests
#sbt gatling-it:test
#sbt gatling-it:lastReport