#!/usr/bin/env groovy
pipeline {
    agent any 

    stages {
        stage('Prepare') { 
            steps { 
                echo 'Preparing ...' 
            }
        }
        stage('Certify WAR'){
            steps {
                echo 'Certifying WAR ...'
            }
        }
        stage('Promote WAR') {
            steps {
                sh 'jfrog rt cp --url=https://huttermann.jfrog.io/huttermann --apikey=$artifactory_key --flat=true libs-release-local/com/huettermann/web/$version/ libs-releases-staging-local/com/huettermann/web/$version/' 
            }
        }
        stage('Certify Docker Image'){
            steps {
                echo 'Certifying Docker Image ...'
            }
        }
        stage('Promote Docker Image') {
            steps {
sh '''curl -H "X-JFrog-Art-Api:$artifactory_key" -X POST https://huttermann.jfrog.io/huttermann/api/docker/docker-local/v2/promote -H "Content-Type:application/json" -d \'{"targetRepo" : "docker-prod-local", "dockerRepository" : "michaelhuettermann/tomcat7", "tag": "\'$version\'", "copy": true }\'
'''
            }
        }
    }
}
