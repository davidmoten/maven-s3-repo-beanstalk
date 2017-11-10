#!/bin/bash
# fail on error
set -e

: "${CA_PASSWORD?Need to set CA_PASSWORD environment variable}"
: "${MODE?Need to set MODE environment variable}"

mkdir -p target
cd target
echo MODE=$MODE
export EXPIRY_DAYS=3650

# create ca certificate root 
tee ca-config <<EOF
# OpenSSL configuration file.
[ req ]
prompt = no
distinguished_name			= req_distinguished_name
 
[ req_distinguished_name ]
C=AU
ST=Some State
L=Some City
CN=mydomain.com
O=My Company
OU=My Section
emailAddress=myemail@mydomain.com
EOF

openssl genrsa -des3 -out ca.key -passout "pass:$CA_PASSWORD" 4096
openssl req -new -x509 -days $EXPIRY_DAYS -key ca.key -out ca.crt -passin "pass:$CA_PASSWORD" -config ca-config 
   
# create server certificates (public and private)
tee server-config <<EOF
# OpenSSL configuration file.
[ req ]
prompt = no
distinguished_name			= req_distinguished_name
 
[ req_distinguished_name ]
C=AU
ST=Some State
L=Some City
CN=mydomain.com
O=My Company
OU=My Section
emailAddress=myemail@mydomain.com
EOF

openssl genrsa -out server-private-key.pem 2048
openssl req -sha256 -new -key server-private-key.pem -out server.csr -config server-config
openssl x509 -req -days $EXPIRY_DAYS -in server.csr -signkey server-private-key.pem -out server.pem -passin "pass:$CA_PASSWORD"
rm server.csr 
   
echo created the following files:
ls ca* server*

echo $CA_PASSWORD > ca-password.txt

# make zip file with all keys
rm -f maven-repo-keys-*.zip
zip maven-repo-keys-$MODE.zip ca.crt ca.key server.pem server-private-key.pem ca-password.txt 
rm ca-password.txt

cp ca.* server.pem server-private-key.* maven-repo-keys-*.zip /tmp

echo ---------------------------------------
echo ---------- IMPORTANT! -----------------
echo ---------------------------------------

echo "You should now save the files below somewhere private and safe:"
echo
echo "    target/maven-repo-keys-$MODE.zip"
echo
echo "Note that if you've lost the contents of target the files have also been copied to /tmp"
 
echo --------------------------------------

cd -
