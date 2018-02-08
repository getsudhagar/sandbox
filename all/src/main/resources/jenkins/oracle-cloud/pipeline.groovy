node {
    withCredentials([string(credentialsId: 'ORACLE_BEARER', variable: 'BEARER')]) {

        def WORKSPACE

        @Library('Util') _

        stage('Prepare') {
            WORKSPACE = pwd()
            echo "where am I ... ${WORKSPACE}"
            echo "which version to process ... ${version}"
            sh "rm -f ${WORKSPACE}/*.json"
            sh "curl -O https://raw.githubusercontent.com/michaelhuettermann/sandbox/master/all/src/main/resources/jenkins/oracle-cloud/new-service.json"
            sh "curl -O https://raw.githubusercontent.com/michaelhuettermann/sandbox/master/all/src/main/resources/jenkins/oracle-cloud/create-deployment.json"
            sh "sed -i '' 's/VERSION/${version}/g' ${WORKSPACE}/new-service.json"
            sh "sed -i '' 's/VERSION/${version}/g' ${WORKSPACE}/create-deployment.json"

            sh 'curl -OL https://bintray.com/huettermann/meow/download_file?file_path=all-${version}-GA.war'
            fingerprint 'all-${version}-GA.war'
        }
        stage('Deployment Stop') {
sh '''
#!/bin/bash 
set +x
export PYTHONIOENCODING=utf8
echo -ne "Stopping deployment "
curl -sk  -X "POST"   -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/deployments/meow-deploy/stop"  
result=$(curl -sk -X 'GET' -H "Authorization: Bearer ${BEARER}" https://${CLOUDIP}/api/v2/deployments/meow-deploy) 
if [[ "$result" == *"errors"* ]]; then
    echo "No deployment found ... !"
else 
    echo "Deployment found ..."
    for (( ; ; ))
    do
       result=$(curl -sk -X 'GET' -H "Authorization: Bearer ${BEARER}" https://${CLOUDIP}/api/v2/deployments/meow-deploy | \
          python -c "import sys, json; print(json.load(sys.stdin)['deployment']['current_state'])")  
       if [ "$result" == "0" ]; then
          echo "Deployment stopped!"
          break
       else
          echo "."
          sleep 2
          continue
       fi
    done
    sleep 5
fi
'''
        }
        stage('Deployment Delete') {
sh '''
#!/bin/bash 
set +x
curl -sk  -X "DELETE" -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/deployments/meow-deploy"
sleep 5
'''
        }
        stage('Service Delete') {
sh '''
#!/bin/bash 
set +x
curl -sk -X  "DELETE" -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/services/meow"
sleep 5
'''
        }
        stage('Image Delete') {
sh '''
#!/bin/bash 
set +x
curl -sk  -X "DELETE" -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/images/${BINTRAYREGISTRY}/michaelhuettermann/alpine-tomcat7:${version}/hosts/2970cd1b-5571-6fda-3f21-1c6b19cd9ab1"
sleep 5
'''
        }
        stage('Image Pull') {
sh '''
#!/bin/bash 
set +x
curl -sk  -X "POST"   -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/images/${BINTRAYREGISTRY}/michaelhuettermann/alpine-tomcat7:${version}/hosts/2970cd1b-5571-6fda-3f21-1c6b19cd9ab1/pull"
sleep 5
'''
        }
        stage('Service Create') {
echo "workspace = ${WORKSPACE}"
sh '''
#!/bin/bash 
set +x
curl -ski -X "POST" -H "Authorization: Bearer ${BEARER}" "https://${CLOUDIP}/api/v2/services/" \
   --data "@${WORKSPACE}/new-service.json"
sleep 5
'''
        }
        stage('Deployment Create') {
            echo "workspace = ${WORKSPACE}"
            timeout(time:5, unit:'MINUTES') {
                input message:"Really sure to go the very last mile, with version ${version}?"
            }
sh '''
#!/bin/bash 
set +x
curl -ski -X "POST" -H "Authorization: Bearer ${BEARER}" "https://${CLOUDIP}/api/v2/deployments/" \
   --data "@${WORKSPACE}/create-deployment.json" 
sleep 5 
'''
        }
    }
}