##
## Provider Definition
##
provider "aws" {
  version = "~> 2.12"
  region = "${var.aws_region}"
  access_key = "${var.aws_access_key}"
  secret_key = "${var.aws_secret_key}"
}

data "aws_vpc" "main" {
  id = "${var.aws_vpc_id}"
}

data "aws_subnet" "main" {
  id = "${var.aws_subnet_id}"
}

resource "aws_s3_bucket" "h2o_bucket" {
  acl = "private"
  force_destroy = true
  tags = {
    Name = "H2ODeploymentBucket"
  }
}

resource "aws_s3_bucket_object" "install_h2o" {
  bucket = "${aws_s3_bucket.h2o_bucket.id}"
  key = "install_h2o.sh"
  acl = "private"
  content = <<EOF

#!/bin/bash
set -x -e

mkdir -p /home/hadoop/h2o
cd /home/hadoop/h2o

wget http://h2o-release.s3.amazonaws.com/h2o/rel-${var.h2o_codename}/${var.h2o_fix_version}/h2o-${var.h2o_main_version}.${var.h2o_fix_version}-hdp2.6.zip
unzip -o h2o*.zip 1> /dev/null & wait
aws s3 cp ${format("s3://%s/realm.properties", aws_s3_bucket.h2o_bucket.bucket)} realm.properties
keytool -genkey -keyalg RSA -keystore h2o.jks -keypass h2oh2o -storepass h2oh2o -keysize 2048 -dname "CN=John Smith, OU=H2O, O=H2O.ai, L=Mountain View, S=California, C=US"

EOF
}

resource "aws_s3_bucket_object" "login_conf" {
  bucket = "${aws_s3_bucket.h2o_bucket.id}"
  key = "realm.properties"
  acl = "private"
  content = <<EOF
ldaploginmodule {
    org.eclipse.jetty.plus.jaas.spi.LdapLoginModule required
    debug="true"
    useLdaps="false"
    contextFactory="com.sun.jndi.ldap.LdapCtxFactory"
    hostname="ldap.jumpcloud.com"
    port="389"
    userIdAttribute="uid"
    bindDn="uid=root,ou=Users,o=5d65ae3675169a4a9130f75b,dc=jumpcloud,dc=com"
    bindPassword="password"
    authenticationMethod="simple"
    forceBindingLogin="true"
    userBaseDn="ou=Users,o=5d65ae3675169a4a9130f75b,dc=jumpcloud,dc=com";
};
EOF
}

resource "aws_emr_cluster" "h2o-cluster" {
  name = "H2O"
  release_label = "${var.aws_emr_version}"
  log_uri = "s3://${aws_s3_bucket.h2o_bucket.bucket}/"
  applications = ["Hadoop"]

  ec2_attributes {
    subnet_id = "${data.aws_subnet.main.id}"
    emr_managed_master_security_group = "${aws_security_group.master.id}"
    emr_managed_slave_security_group = "${aws_security_group.slave.id}"
    instance_profile = "${aws_iam_instance_profile.emr_ec2_instance_profile.arn}"
  }

  master_instance_group {
    instance_type = "${var.aws_instance_type}"
  }

  core_instance_group {
    instance_type = "${var.aws_instance_type}"
    instance_count = "${var.aws_core_instance_count}"
  }

  tags = {
    name = "H2O"
  }

  bootstrap_action {
    path = "${format("s3://%s/install_h2o.sh", aws_s3_bucket.h2o_bucket.bucket)}"
    name = "Custom action"
  }

  step {
    action_on_failure = "TERMINATE_CLUSTER"
    name   = "Start H2O"

    hadoop_jar_step {
      jar  = "/home/hadoop/h2o/h2o-${var.h2o_main_version}.${var.h2o_fix_version}-hdp2.6/h2odriver.jar"
      args = ["-n", "${var.aws_core_instance_count}", "-mapperXmx", "${var.h2o_mapper_xmx}", "-proxy", "-ldap_login", "-login_conf", "/home/hadoop/h2o/realm.properties", "-form_auth", "-port", "54321", "-jks", "/home/hadoop/h2o/h2o.jks", "-user_name", "${var.h2o_user_name}"]
    }
  }

  configurations_json = <<EOF
  [
    {
      "Classification": "hadoop-env",
      "Configurations": [
        {
          "Classification": "export",
          "Properties": {
            "JAVA_HOME": "/usr/lib/jvm/java-1.8.0"
          }
        }
      ],
      "Properties": {}
    }
  ]
EOF
  provisioner "local-exec" {
    command = "sleep 60"
  }
  service_role = "${aws_iam_role.emr_role.arn}"
}
