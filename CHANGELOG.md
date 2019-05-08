# Auguts 2018

## Geogig Web

* Also clone spatial indexes efficiently when forking a repository
* Made DiffCount and DiffBounds able to compute against branches from different repositories
* Finish JPA storage for all the configuration services
* Allow to rescue orphan repositories in a database/directory and assign ownership to an existing user
* Define API and semantics of conflict resolution for branch and pull request merge

## TODO:

* Prepare for BETA release

# July 2018

## Geogig

* Geogig PostgreSQL backend uses (WAL safe) HASH index if server version >= 10.0
* Split Postgres DDL script in two phases: create and execute, allowing to create the script to be run by a DBA instead of by geogig
* Fix PostgreSQL backend's graph database parent traversal order (worked only on Postgres 9.4 by accident)
* Refactoring of geogig core modules so downstream projects don't need to depend con its CLI
* RevTree building performance and memory footprint improvements
* Shared cache stores uncompressed objects, as LZ4 compression as too much overhead especially under load

## Geogig Web

* Upgrade to Geogig 1.4.x
* Progress reporting for AsyncTasks
* Split server into service modules in anticipation of migration to a microservices architecture. Remove several interdependencies.
* Stop depending on geogig-cli for the server
* Implement update for pull requests
* Protocol buffers encoding for diff results
* Implement JPA storage for all the configuration services and get rid of the prototype JSON files backed ones
* Decouple service model from presentation model
