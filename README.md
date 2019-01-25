## Manual Setup

#### OC and Minishift

Follow the Minishift install instructions according to your environment:

https://docs.okd.io/latest/minishift/getting-started/preparing-to-install.html

Install the most recent `oc` binary:

https://github.com/openshift/origin/releases

Ensure that Minishift and `oc` versions are aligned:
```
$ oc version
oc v3.11.0+0cbc58b
kubernetes v1.11.0+d4cacc0
features: Basic-Auth

$ minishift status
Server https://192.168.64.6:8443
kubernetes v1.11.0+d4cacc0
```

#### Start the Minishift VM and Enable Admin User

Start minishift with enough resources:
```
$ minishift start --cpus 4 --disk-size 100GB --memory 12GB
```

Once the Kubernetes cluster is running, login as admin user:
```
$ oc login -u system:admin
```

Enable the admin user so that you can login to the console as u:admin, p:admin
```
$ minishift addon apply admin-user
```
You should be able to login to the web console with user:admin, pass:admin

Create a new project for the demo:
```
$ oc new-project rtp-reference
```


#### AMQ Streams Cluster and Kafka Topics

Note: Installation of the AMQ Streams Kafka cluster requires an OpenShift user with the cluster-admin role.

Apply the Cluster Operator installation file:
```
$ oc apply -f kafka/install/cluster-operator/deployment-srtimzi-cluster-operator.yaml -n rtp-reference
```

Provision an ephemeral Kafka cluster:
```
$ oc apply -f kafka/install/cluster/kafka-ephemeral.yaml -n rtp-reference
```

Watch the deployment until all Kafka pods are created and running:
```
$ oc get pods -w -n rtp-reference
NAME                                          READY     STATUS    RESTARTS   AGE
my-cluster-entity-operator-5d7cd7774c-x8sg7   3/3       Running   0          33s
my-cluster-kafka-0                            2/2       Running   0          57s
my-cluster-kafka-1                            2/2       Running   0          57s
my-cluster-kafka-2                            2/2       Running   0          57s
my-cluster-zookeeper-0                        2/2       Running   0          1m
my-cluster-zookeeper-1                        2/2       Running   0          1m
my-cluster-zookeeper-2                        2/2       Running   0          1m
strimzi-cluster-operator-56d699b5c5-ch9r2     1/1       Running   0          2m
```

Create the topics for the rtp demo application:
```
$ oc apply -f kafka/install/topics/creditor-completed-payments.yaml -n rtp-reference
$ oc apply -f kafka/install/topics/creditor-payment-confirmation.yaml -n rtp-reference
$ oc apply -f kafka/install/topics/creditor-payments.yaml -n rtp-reference
$ oc apply -f kafka/install/topics/debtor-completed-payments.yaml -n rtp-reference
$ oc apply -f kafka/install/topics/debtor-payment-confirmation.yaml -n rtp-reference
$ oc apply -f kafka/install/topics/debtor-payments.yaml -n rtp-reference
$ oc apply -f kafka/install/topics/mock-rtp-creditor-acknowledgment.yaml -n rtp-reference
$ oc apply -f kafka/install/topics/mock-rtp-creditor-confirmation.yaml -n rtp-reference
$ oc apply -f kafka/install/topics/mock-rtp-creditor-credit-transfer.yaml -n rtp-reference
$ oc apply -f kafka/install/topics/mock-rtp-debtor-confirmation.yaml -n rtp-reference
$ oc apply -f kafka/install/topics/mock-rtp-debtor-credit-transfer.yaml -n rtp-reference
```

Confirm on each Kafka broker that the topics were replicated.
```
$ oc exec -it rtp-demo-cluster-kafka-0 -c kafka -- bin/kafka-topics.sh --zookeeper localhost:2181 --list
creditor-completed-payments
creditor-payment-confirmation
creditor-payments
debtor-completed-payments
debtor-payment-confirmation
debtor-payments
mock-rtp-creditor-acknowledgement
mock-rtp-creditor-confirmation
mock-rtp-creditor-credit-transfer
mock-rtp-debtor-confirmation
mock-rtp-debtor-credit-transfer
```
```
$ oc exec -it rtp-demo-cluster-kafka-1 -c kafka -- bin/kafka-topics.sh --zookeeper localhost:2181 --list
creditor-completed-payments
creditor-payment-confirmation
creditor-payments
debtor-completed-payments
debtor-payment-confirmation
debtor-payments
mock-rtp-creditor-acknowledgement
mock-rtp-creditor-confirmation
mock-rtp-creditor-credit-transfer
mock-rtp-debtor-confirmation
mock-rtp-debtor-credit-transfer
```
```
$ oc exec -it rtp-demo-cluster-kafka-2 -c kafka -- bin/kafka-topics.sh --zookeeper localhost:2181 --list
creditor-completed-payments
creditor-payment-confirmation
creditor-payments
debtor-completed-payments
debtor-payment-confirmation
debtor-payments
mock-rtp-creditor-acknowledgement
mock-rtp-creditor-confirmation
mock-rtp-creditor-credit-transfer
mock-rtp-debtor-confirmation
mock-rtp-debtor-credit-transfer
```



#### JBoss Datagrid Server and Caches

Import the JDG OpenShift image:
```
$ oc import-image -n openshift registry.access.redhat.com/jboss-datagrid-7/datagrid72-openshift --confirm
```

Confirm the image was imported:
```
$ oc get is -n openshift
NAME                   DOCKER REPO                                      TAGS                         UPDATED
datagrid72-openshift   172.30.1.1:5000/openshift/datagrid72-openshift   latest                       41 hours ago
```

Create the JDG server and caches:
```
$ oc new-app --name=rtp-demo-cache \
--image-stream=datagrid72-openshift:latest \
-e INFINISPAN_CONNECTORS=hotrod \
-e CACHE_NAMES=debtorAccountCache,creditorAccountcache \
-e HOTROD_SERVICE_NAME=rtp-demo-cache\
-e HOTROD_AUTHENTICATION=true \
-e USERNAME=jdguser \
-e PASSWORD=P@ssword1
```


#### Deploy the RTP Reference Services

Capture the bootstrap IP for the Kafka cluster and add port 9092:
```
$ bootstrap=`oc get service rtp-demo-cluster-kafka-bootstrap -o=jsonpath='{.spec.clusterIP}{"\n"}'`
$ bootstrap="${bootstrap}:9092"
```

Build, configure and deploy the Debtor Payment Service:
```
$ cd rtp-debtor-payment-service
$ oc create configmap rtp-debtor-payment-service-config \
            --from-literal=BOOTSTRAP_SERVERS="${bootstrap}" \
            --from-literal=PRODUCER_TOPIC=debtor-payments \
            --from-literal=SECURITY_PROTOCOL=PLAINTEXT \
            --from-literal=SERIALIZER_CLASS=rtp.demo.debtor.domain.model.payment.serde.PaymentSerializer \
            --from-literal=ACKS=1
$ mvn fabric8:deploy -Popenshift
$ oc set env dc/rtp-debtor-payment-service --from configmap/rtp-debtor-payment-service-config
$ cd ..
```

Build, configure and deploy the Debtor Send Payment Service
```
$ cd rtp-debtor-send-payment
$ oc create configmap rtp-debtor-send-payment-config \
            --from-literal=BOOTSTRAP_SERVERS="${bootstrap}" \
            --from-literal=CONSUMER_TOPIC=debtor-payments \
            --from-literal=PRODUCER_TOPIC=mock-rtp-debtor-credit-transfer \
            --from-literal=CONSUMER_MAX_POLL_RECORDS=500 \
            --from-literal=CONSUMER_COUNT=1 \
            --from-literal=CONSUMER_SEEK_TO=end \
            --from-literal=CONSUMER_GROUP=rtp-debtor-send-payment \
            --from-literal=SECURITY_PROTOCOL=PLAINTEXT \
            --from-literal=DESERIALIZER_CLASS=rtp.message.model.serde.FIToFICustomerCreditTransferV06Deserializer \
            --from-literal=SERIALIZER_CLASS=rtp.demo.debtor.domain.model.payment.serde.PaymentSerializer \
            --from-literal=ACKS=1
$ mvn fabric8:deploy -Popenshift
$ oc set env dc/rtp-debtor-send-payment --from configmap/rtp-debtor-send-payment-config
$ cd ..
```

Build, configure and deploy the Mock RTP Service

```
$ cd rtp-mock
$ oc create configmap rtp-mock-config \
            --from-literal=BOOTSTRAP_SERVERS="${bootstrap}" \
            --from-literal=SECURITY_PROTOCOL=PLAINTEXT \
            --from-literal=CREDIT_TRANS_DEBTOR_TOPIC=mock-rtp-debtor-credit-transfer \
            --from-literal=CREDIT_TRANS_CREDITOR_TOPIC=mock-rtp-creditor-credit-transfer \
            --from-literal=CREDITOR_ACK_TOPIC=mock-rtp-creditor-acknowledgment \
            --from-literal=DEBTOR_CONFIRMATION_TOPIC=mock-rtp-debtor-confirmation \
            --from-literal=CREDITOR_CONFIRMATION_TOPIC=mock-rtp-creditor-confirmation \
            --from-literal=CONSUMER_MAX_POLL_RECORDS=500 \
            --from-literal=CONSUMER_COUNT=1 \
            --from-literal=CONSUMER_SEEK_TO=end \
            --from-literal=CONSUMER_GROUP=rtp-mock \
            --from-literal=ACKS=1
$ mvn fabric8:deploy -Popenshift
$ oc set env dc/rtp-mock --from configmap/rtp-mock-config
$ cd ..
```

Build, configure and deploy the Creditor Receive Payment Service

```
$ cd rtp-creditor-receive-payment
$ oc create configmap rtp-creditor-receive-payment-config \
            --from-literal=BOOTSTRAP_SERVERS="${bootstrap}" \
            --from-literal=SECURITY_PROTOCOL=PLAINTEXT \
            --from-literal=CREDIT_TRANS_CREDITOR_TOPIC=mock-rtp-creditor-credit-transfer \
            --from-literal=CREDITOR_PAYMENTS_TOPIC=creditor-payments \
            --from-literal=CONSUMER_MAX_POLL_RECORDS=500 \
            --from-literal=CONSUMER_COUNT=1 \
            --from-literal=CONSUMER_SEEK_TO=end \
            --from-literal=CONSUMER_GROUP=rtp-mock \
            --from-literal=ACKS=1
$ mvn fabric8:deploy -Popenshift
$ oc set env dc/rtp-creditor-receive-payment --from configmap/rtp-creditor-receive-payment-config
$ cd ..
```





# rtp-reference

















docker login registry.redhat.io


cat ~/.docker/config.json

oc project myproject

oc create secret generic pull-secret-name   --from-file=.dockerconfigjson=/Users/liz/.docker/config.json   --type=kubernetes.io/dockerconfigjson

oc secrets link default pull-secret-name --for=pull

oc secrets link builder pull-secret-name


for resource in datagrid72-image-stream.json \
  datagrid72-basic.json \
  datagrid72-https.json \
  datagrid72-mysql-persistent.json \
  datagrid72-mysql.json \
  datagrid72-partition.json \
  datagrid72-postgresql.json \
  datagrid72-postgresql-persistent.json
do
  oc create -n openshift -f \
  https://raw.githubusercontent.com/jboss-container-images/jboss-datagrid-7-openshift-image/1.3/templates/${resource}
done

oc get templates -n openshift | grep datagrid72


oc get is -n openshift | grep datagrid

docker pull registry.access.redhat.com/jboss-datagrid-7/datagrid72-openshift

oc -n openshift import-image jboss-datagrid72-openshift:1.3



------------

minishift docker-env

eval $(minishift docker-env)

docker pull registry.access.redhat.com/jboss-datagrid-7/datagrid72-openshift

for resource in datagrid72-image-stream.json \
  datagrid72-basic.json \
  datagrid72-https.json \
  datagrid72-mysql-persistent.json \
  datagrid72-mysql.json \
  datagrid72-partition.json \
  datagrid72-postgresql.json \
  datagrid72-postgresql-persistent.json
do
  oc create -n openshift -f \
  https://raw.githubusercontent.com/jboss-container-images/jboss-datagrid-7-openshift-image/1.3/templates/${resource}
done

oc get templates -n openshift | grep datagrid72

oc describe is jboss-datagrid72-openshift -n openshift



---------------------------------
oc -n openshift import-image registry.access.redhat.com/jboss-datagrid-7/datagrid72-openshift --confirm


oc describe template datagrid72-basic -n openshift

oc new-app --template=datagrid72-basic --name=rhdg \
  -e USERNAME=developer -e PASSWORD=password \
  -e CACHE_NAMES=mycache -e MYCACHE_CACHE_START=EAGER



-------------------------------------

minishift start --cpus 4 --disk-size 100GB --memory 12GB



oc -n openshift import-image registry.access.redhat.com/jboss-datagrid-7/datagrid72-openshift --confirm



oc new-project datagrid

oc new-app --template=datagrid72-basic --name=rhdg \
  -e CACHE_NAMES=mycache -e MYCACHE_CACHE_START=EAGER


  curl -i -H "Accept:application/json" \
  http://datagrid-app-datagrid.192.168.64.5.nip.io/rest/default/a




  curl -X POST -i -H "Content-type:application/json" \
  -d "{\"name\":\"Red Hat Data Grid\"}" \
  http://datagrid-app-datagrid.192.168.64.5.nip.io/rest/mycache/a



----
oc -n openshift import-image registry.access.redhat.com/jboss-eap-7/eap71-openshift:1.2

jboss-eap-7/eap71-openshift:1.2

----

oc -n openshift import-image my-jboss-eap-7/eap71-openshift --from=registry.access.redhat.com/jboss-eap-7/eap71-openshift --confirm

oc new-build --binary=true --image-stream=eap71-openshift:latest --name=eap-app

oc start-build eap-app --from-dir=deployments/ --follow

---













oc create -n openshift secret docker-registry imagestreamsecret \
  --docker-server=registry.redhat.io \
  --docker-username=espangle@redhat.com \
  --docker-password=EniTln23= \
  --docker-email=espangle@redhat.com


  BASEURL=https://raw.githubusercontent.com/jboss-fuse/application-templates/application-templates-2.1.fuse-000064-redhat-4

  oc create -n openshift -f ${BASEURL}/fis-image-streams.json

  oc replace -n openshift -f ${BASEURL}/fis-image-streams.json


  for template in eap-camel-amq-template.json \
   eap-camel-cdi-template.json \
   eap-camel-cxf-jaxrs-template.json \
   eap-camel-cxf-jaxws-template.json \
   eap-camel-jpa-template.json \
   karaf-camel-amq-template.json \
   karaf-camel-log-template.json \
   karaf-camel-rest-sql-template.json \
   karaf-cxf-rest-template.json \
   spring-boot-camel-amq-template.json \
   spring-boot-camel-config-template.json \
   spring-boot-camel-drools-template.json \
   spring-boot-camel-infinispan-template.json \
   spring-boot-camel-rest-sql-template.json \
   spring-boot-camel-teiid-template.json \
   spring-boot-camel-template.json \
   spring-boot-camel-xa-template.json \
   spring-boot-camel-xml-template.json \
   spring-boot-cxf-jaxrs-template.json \
   spring-boot-cxf-jaxws-template.json ;
   do
   oc create -n openshift -f \
   https://raw.githubusercontent.com/jboss-fuse/application-templates/application-templates-2.1.fuse-720018-redhat-00001/quickstarts/${template}
   done


   oc create -n openshift -f https://raw.githubusercontent.com/jboss-fuse/application-templates/application-templates-2.1.fuse-720018-redhat-00001/fis-console-cluster-template.json


oc create -n openshift -f https://raw.githubusercontent.com/jboss-fuse/application-templates/application-templates-2.1.fuse-720018-redhat-00001/fis-console-namespace-template.json


oc create -n openshift -f ${BASEURL}/fuse-apicurito.yml


oc get template -n openshift


mvn fabric8:deploy -Popenshift




**
for resource in datagrid72-image-stream.json \
  datagrid72-basic.json \
  datagrid72-https.json \
  datagrid72-mysql-persistent.json \
  datagrid72-mysql.json \
  datagrid72-partition.json \
  datagrid72-postgresql.json \
  datagrid72-postgresql-persistent.json
do
  oc create -n openshift -f \
  https://raw.githubusercontent.com/jboss-container-images/jboss-datagrid-7-openshift-image/1.3/templates/${resource}
done



 oc new-app --name=carcache-hotrod --image-stream=jboss-datagrid72-openshift:1.3 -e INFINISPAN_CONNECTORS=hotrod -e CACHE_NAMES=carcache -e HOTROD_SERVICE_NAME=carcache-hotrod -e HOTROD_AUTHENTICATION=true -e USERNAME=jdguser -e PASSWORD=P@ssword1


 oc new-app --name=carcache-hotrod \
 --image-stream=jboss-datagrid72-openshift:1.3 \
 -e INFINISPAN_CONNECTORS=hotrod \
 -e CACHE_NAMES=carcache \
 -e HOTROD_SERVICE_NAME=carcache-hotrod \
 -e HOTROD_AUTHENTICATION=true \
 -e USERNAME=jdguser \
 -e PASSWORD=P@ssword1


*****
oc import-image -n openshift registry.access.redhat.com/jboss-eap-7/eap71-openshift --confirm



oc new-build --binary=true \
--image-stream=eap71-openshift:latest \
--name=eap-app


oc start-build eap-app --from-dir=deployments/ --follow

oc new-app eap-app

















==========================================

oc import-image -n openshift registry.access.redhat.com/jboss-eap-7/eap71-openshift --confirm



==========================================
==========================================
==========================================

Create a docker-registry secret using either Red Hat Customer Portal account or Red Hat Developer Program account credentials.
```
oc create secret docker-registry imagestreamsecret \
  --docker-server=registry.redhat.io \
  --docker-username=CUSTOMER_PORTAL_USERNAME \
  --docker-password=CUSTOMER_PORTAL_PASSWORD \
  --docker-email=EMAIL_ADDRESS
```

// fuse image and templates

```
$ BASEURL=https://raw.githubusercontent.com/jboss-fuse/application-templates/application-templates-2.1.fuse-720018-redhat-00001
$ oc create -n openshift -f ${BASEURL}/fis-image-streams.json
```

```
$ for template in eap-camel-amq-template.json \
 eap-camel-cdi-template.json \
 eap-camel-cxf-jaxrs-template.json \
 eap-camel-cxf-jaxws-template.json \
 eap-camel-jpa-template.json \
 karaf-camel-amq-template.json \
 karaf-camel-log-template.json \
 karaf-camel-rest-sql-template.json \
 karaf-cxf-rest-template.json \
 spring-boot-camel-amq-template.json \
 spring-boot-camel-config-template.json \
 spring-boot-camel-drools-template.json \
 spring-boot-camel-infinispan-template.json \
 spring-boot-camel-rest-sql-template.json \
 spring-boot-camel-teiid-template.json \
 spring-boot-camel-template.json \
 spring-boot-camel-xa-template.json \
 spring-boot-camel-xml-template.json \
 spring-boot-cxf-jaxrs-template.json \
 spring-boot-cxf-jaxws-template.json ;
 do
 oc create -n openshift -f \
 https://raw.githubusercontent.com/jboss-fuse/application-templates/application-templates-2.1.fuse-720018-redhat-00001/quickstarts/${template}
 done
 ```

  ```
$ oc create -n openshift -f https://raw.githubusercontent.com/jboss-fuse/application-templates/application-templates-2.1.fuse-720018-redhat-00001/fis-console-cluster-template.json
$ oc create -n openshift -f https://raw.githubusercontent.com/jboss-fuse/application-templates/application-templates-2.1.fuse-720018-redhat-00001/fis-console-namespace-template.json
$ oc create -n openshift -f ${BASEURL}/fuse-apicurito.yml
 ```

 ```
 $ oc get template -n openshift
 ```


// datagrid image
```
$ oc import-image -n openshift registry.access.redhat.com/jboss-datagrid-7/datagrid72-openshift --confirm
$ oc get is -n openshift
```



###







```
$ oc new-app --name=rtp-creditor-cache \
--image-stream=datagrid72-openshift:latest \
-e INFINISPAN_CONNECTORS=hotrod \
-e CACHE_NAMES=accountcache \
-e HOTROD_SERVICE_NAME=rtp-creditor-cache\
-e HOTROD_AUTHENTICATION=true \
-e USERNAME=jdguser \
-e PASSWORD=P@ssword1
```









curl -X POST -i -H "Content-type:application/json" \
-d "{\"name\":\"Red Hat Data Grid\"}" \
http://rhdgroute-datagrid.192.0.2.0.nip.io/rest/mycache/a


curl -X POST -i -H "Content-type:application/json" \
{\"accountNumber\":\"12000194212199001\",\"accountType\":\"Checking\"} \
http://rtp-creditor-cache-rtp-test.192.168.64.7.nip.io/rest/mycache/12000194212199001



{
  "accountNumber":"12000194212199001",
  "accountType":"Checking"
}




oc new-app --name=rtpcache-hotrod --image-stream=jboss-datagrid72-openshift:1.3 -e INFINISPAN_CONNECTORS=hotrod -e CACHE_NAMES=rtpcache -e HOTROD_SERVICE_NAME=rtpcache-hotrod -e HOTROD_AUTHENTICATION=true -e USERNAME=jdguser -e PASSWORD=P@ssword1





### Resources

https://access.redhat.com/documentation/en-us/red_hat_fuse/7.2/html-single/fuse_on_openshift_guide/
https://access.redhat.com/documentation/en-us/red_hat_jboss_data_grid/7.2/html-single/data_grid_for_openshift/




















public static class TestCallbackHandler implements CallbackHandler {
  final private String username;
  final private char[] password;
  final private String realm;

  public TestCallbackHandler(String username, String realm, char[] password) {
    this.username = username;
    this.password = password;
    this.realm = realm;
  }

  @Override
  public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
    for (Callback callback : callbacks) {
      if (callback instanceof NameCallback) {
        NameCallback nameCallback = (NameCallback) callback;
        nameCallback.setName(username);
      } else if (callback instanceof PasswordCallback) {
        PasswordCallback passwordCallback = (PasswordCallback) callback;
        passwordCallback.setPassword(password);
      } else if (callback instanceof AuthorizeCallback) {
        AuthorizeCallback authorizeCallback = (AuthorizeCallback) callback;
        authorizeCallback.setAuthorized(
            authorizeCallback.getAuthenticationID().equals(authorizeCallback.getAuthorizationID()));
      } else if (callback instanceof RealmCallback) {
        RealmCallback realmCallback = (RealmCallback) callback;
        realmCallback.setText(realm);
      } else {
        throw new UnsupportedCallbackException(callback);
      }
    }
  }
}




https://github.com/oscerd/camel-infinispan-kafka-demo/blob/master/src/main/java/com/github/oscerd/camel/infinispan/kafka/demo/Application.java













Verify the image
```
oc -n openshift import-image registry.access.redhat.com/jboss-datagrid-7/datagrid72-openshift --confirm
```





https://access.redhat.com/documentation/en-us/red_hat_amq/7.2/html-single/using_amq_streams_on_openshift_container_platform/






{
  "payments":[
    {
  		"debtorAccountNumber":"1234567890123456",
  		"amount":"20.00",
  		"receiverFirstName":"John",
  		"receiverLastName":"Smith"
	},
	{
 		 "debtorAccountNumber":"1234567890123456",
 		 "amount":"100.25",
 		 "receiverFirstName":"Amy",
 		 "receiverLastName":"Lopez"
    }
  ]
}


  .