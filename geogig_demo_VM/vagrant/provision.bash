echo PROVISIONING...

echo JAVA
	  wget https://download.java.net/java/GA/jdk10/10.0.2/19aef61b38124481863b1413dce1855f/13/openjdk-10.0.2_linux-x64_bin.tar.gz
	  tar xzf openjdk-10.0.2_linux-x64_bin.tar.gz
	  sudo mkdir /usr/lib/jvm/
	  sudo mv jdk-10.0.2 /usr/lib/jvm/java-10-openjdk-amd64/
	  sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/java-10-openjdk-amd64/bin/java 1
	  sudo update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/java-10-openjdk-amd64/bin/javac 1

echo Postgresql 10
	wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
	sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt/ xenial-pgdg main" > /etc/apt/sources.list.d/pgdg_xenial.list'
	sudo apt update --yes
	sudo apt-get install --yes postgresql-10 
	sudo systemctl enable postgresql.service
	sudo systemctl stop postgresql.service
	sleep 5


	sudo sed -i "s/#listen_addresses = 'localhost'/listen_addresses = '*'/" /etc/postgresql/10/main/postgresql.conf
	#echo "client_encoding = utf8"  | sudo tee -a /etc/postgresql/10/main/postgresql.conf

	echo "host    all             all             all                     trust" | sudo tee  /etc/postgresql/10/main/pg_hba.conf
	echo "local   all             postgres                                trust" | sudo tee  -a /etc/postgresql/10/main/pg_hba.conf
	echo "local   all             all                                     trust" | sudo tee  -a /etc/postgresql/10/main/pg_hba.conf

	
	sudo chown -R postgres:postgres /home/vagrant/main
	sudo chmod -R o= /home/vagrant/main
	sudo chmod -R g= /home/vagrant/main
	sudo rm -r /var/lib/postgresql/10/main/
	sudo mv /home/vagrant/main /var/lib/postgresql/10/
	sudo systemctl start postgresql.service

echo GEOGIG
	sudo systemctl enable geogig
	sudo systemctl start geogig


echo PROVISIONING...DONE


#psql --host localhost --port 5432 -U postgres
#sudo apt-get install secure-delete
#sudo sfill -l -z /
