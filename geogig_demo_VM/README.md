 
GeoGIG DEMO VM
==============

Basic Usage
-----------

1. Put a PG database in a directory called "main" (same place as the Vagrantfile)
2. Put a GeoGig WebAPI configuration in the directory called "GEOGIG_CONFIG" (same place as the Vagrantfile)
3. Put the combined jar (geogig-server-app-0.2-SNAPSHOT.jar) in the same place as the Vagrantfile
      cp ../modules/java/server/app/target/geogig-server-app-0.2-SNAPSHOT.jar .


1. on the host machine, use "vagrant ssh" to shell onto the VM
2. on the host machine, use "localhost:8181" to connect to swagger UI
3. on the host machine, use "psql --host localhost --port 5432 -U postgres" to connect to the database ("geogig")



To get the DB from the VM
-------------------------

NOTE: will have to share a directory with the host machine (configure in virtual box and restart the VM)


1. vagrant ssh # open a shell on the VM
2. sudo systemctl stop postgresql.service
3. rm -r /vagrant/main   # this will remove FROM THE HOST MACHINE!!
4. sleep 10 # allow time for DB to come down
5. sudo cp -r /var/lib/postgresql/10/main /vagrant


On the host machine, you should see the "main" directory in the same location as the Vagrantfile

NOTE: you probably want to also get the GeoGig config (see below)!


To get the GeoGig Config from the VM
-------------------------------------

NOTE: will have to share a directory with the host machine (configure in virtual box and restart the VM)


1. sudo systemctl stop geogig
2. rm -r /vagrant/GEOGIG_CONFIG  # this will remove FROM THE HOST MACHINE!!
3. sudo cp -r GEOGIG_CONFIG /vagrant


On the host machine, you should see the "GEOGIG_CONFIG" directory in the same location as the Vagrantfile

NOTE: you probably want to also get the PG database (see above)!


Create OVA
----------

1. vagrant halt  
2. go to virtual box GUI
3. You should see the GeoGig box (shutdown) - select it
4. in the menus, export appliance (OVA 1.0)

