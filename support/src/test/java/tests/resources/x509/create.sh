#!/bin/bash -
# Copyright (C) 2012 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -o nounset                              # Treat unset variables as an error
set -e

DIR=$(dirname $0)

openssl req -config ${DIR}/default.cnf -new -nodes -batch > cert-rsa-req.pem
openssl req -in cert-rsa-req.pem -pubkey -noout | openssl rsa -pubin -pubout -outform der > cert-rsa-pubkey.der
openssl x509 -extfile ${DIR}/default.cnf -days 3650 -extensions usr_cert -req -signkey privkey.pem -outform d < cert-rsa-req.pem > cert-rsa.der
rm -f cert-rsa-req.pem

openssl asn1parse -in cert-rsa.der -inform d -out cert-rsa-tbs.der -noout -strparse 4
SIG_OFFSET=$(openssl asn1parse -in cert-rsa.der -inform d | tail -1 | cut -f1 -d:)
openssl asn1parse -in cert-rsa.der -inform d -strparse ${SIG_OFFSET} -noout -out cert-rsa-sig.der

# extract startdate and enddate
openssl x509 -in cert-rsa.der -inform d -noout -startdate -enddate > cert-rsa-dates.txt

# extract serial
openssl x509 -in cert-rsa.der -inform d -noout -serial > cert-rsa-serial.txt

openssl req -config ${DIR}/default.cnf -new -nodes -batch | openssl x509 -extfile ${DIR}/default.cnf -extensions keyUsage_extraLong_cert -req -signkey privkey.pem -outform d > cert-keyUsage-extraLong.der

openssl req -config ${DIR}/default.cnf -new -nodes -batch | openssl x509 -extfile ${DIR}/default.cnf -extensions extendedKeyUsage_cert -req -signkey privkey.pem -outform d > cert-extendedKeyUsage.der

openssl req -config ${DIR}/default.cnf -new -nodes -batch | openssl x509 -extfile ${DIR}/default.cnf -extensions ca_cert -req -signkey privkey.pem -outform d > cert-ca.der

openssl req -config ${DIR}/default.cnf -new -nodes -batch | openssl x509 -extfile ${DIR}/default.cnf -extensions userWithPathLen_cert -req -signkey privkey.pem -outform d > cert-userWithPathLen.der

openssl req -config ${DIR}/default.cnf -new -nodes -batch | openssl x509 -extfile ${DIR}/default.cnf -extensions caWithPathLen_cert -req -signkey privkey.pem -outform d > cert-caWithPathLen.der

openssl req -config ${DIR}/default.cnf -new -nodes -batch | openssl x509 -extfile ${DIR}/default.cnf -extensions alt_other_cert -req -signkey privkey.pem -outform d > cert-alt-other.der

openssl req -config ${DIR}/default.cnf -new -nodes -batch | openssl x509 -extfile ${DIR}/default.cnf -extensions alt_email_cert -req -signkey privkey.pem -outform d > cert-alt-email.der

openssl req -config ${DIR}/default.cnf -new -nodes -batch | openssl x509 -extfile ${DIR}/default.cnf -extensions alt_dns_cert -req -signkey privkey.pem -outform d > cert-alt-dns.der

openssl req -config ${DIR}/default.cnf -new -nodes -batch | openssl x509 -extfile ${DIR}/default.cnf -extensions alt_dirname_cert -req -signkey privkey.pem -outform d > cert-alt-dirname.der

openssl req -config ${DIR}/default.cnf -new -nodes -batch | openssl x509 -extfile ${DIR}/default.cnf -extensions alt_uri_cert -req -signkey privkey.pem -outform d > cert-alt-uri.der

openssl req -config ${DIR}/default.cnf -new -nodes -batch | openssl x509 -extfile ${DIR}/default.cnf -extensions alt_rid_cert -req -signkey privkey.pem -outform d > cert-alt-rid.der

openssl req -config ${DIR}/default.cnf -new -nodes -batch | openssl x509 -extfile ${DIR}/default.cnf -extensions ipv6_cert -req -signkey privkey.pem -outform d > cert-ipv6.der

openssl req -config ${DIR}/default.cnf -new -nodes -batch | openssl x509 -extfile ${DIR}/default.cnf -extensions unsupported_cert -req -signkey privkey.pem -outform d > cert-unsupported.der

openssl dsaparam -out dsaparam.pem 1024
openssl req -config ${DIR}/default.cnf -newkey dsa:dsaparam.pem -keyout dsapriv.pem -nodes -batch | openssl x509 -extfile ${DIR}/default.cnf -extensions keyUsage_cert -req -signkey dsapriv.pem -outform d > cert-dsa.der
rm -f dsaparam.pem

openssl ecparam -name sect283k1 -out ecparam.pem
openssl req -config ${DIR}/default.cnf -newkey ec:ecparam.pem -keyout ecpriv.pem -nodes -batch | openssl x509 -extfile ${DIR}/default.cnf -extensions keyUsage_critical_cert -req -signkey ecpriv.pem -outform d > cert-ec.der
rm -f ecparam.pem

rm -f privkey.pem
rm -f dsapriv.pem
rm -f ecpriv.pem
