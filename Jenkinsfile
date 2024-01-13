pipeline {
    agent any
    environment {
        NPM_CONFIG_CACHE = "${WORKSPACE}/.npm"
    }
    stages {
        stage('Build') {
            agent {
                docker { 
                    image 'maven:3-openjdk-8'
                    reuseNode true
                }
            }
            steps {
                // Run Maven on a Unix agent.
                sh 'mvn -Dmaven.test.failure.ignore=true clean package -DversionHash=$(git rev-parse --short HEAD)'
                sh "cp target/a2lparser-*-jar-with-dependencies.jar a2lparser.jar"
                sh "java -jar a2lparser.jar -jsc -o a2lSchema.json"
                sh "java -jar a2lparser.jar -a2l src/test/resources/freeTest.a2l -c ISO-8859-1 -mj -o freeTestA2l.minified.json"
            }

            post {
                // If Maven was able to run the tests, even if some of the test
                // failed, record the test results and archive the jar file.
                success {
                    junit '**/target/surefire-reports/TEST-*.xml'
                    archiveArtifacts 'target/*.jar'
                    archiveArtifacts 'a2lSchema.json'
                }
            }
        }
        
        stage('C# codegen') {
            agent {
                docker { 
                    image 'mcr.microsoft.com/dotnet/sdk:6.0-alpine'
                    reuseNode true
                }
            }
            environment {
                HOME = '/tmp'
            } 
            steps {
                dir("codegen_csharp") {
                    sh "dotnet run --project A2lParserCodeGenerator.csproj ../a2lSchema.json"
                    sh "dotnet run --project A2lParserSample.csproj ../freeTestA2l.minified.json"
                }
            }
            
            post {
                success {
                    archiveArtifacts 'codegen_csharp/A2l.cs'
                }
            }
        }
        
        stage('Python codegen') {
            agent {
                docker { 
                    image 'python:3.11-slim'
                    args '-u root'
                    reuseNode true
                }
            }
            steps {
                dir("codegen_python") {
                    sh "pip3 install -r requirements.txt"
                    sh "python3 generatePythonCode.py ../a2lSchema.json"
                    sh "python3 sample.py ../freeTestA2l.minified.json"
                }
            }
            
            post {
                success {
                    archiveArtifacts 'codegen_python/a2l.py'
                }
            }
        }
        
        stage('Typescript codegen') {
            agent {
                docker { 
                    image 'node:lts-alpine'
                    args '-u root'
                    reuseNode true
                }
            }
            steps {
                sh "apk add g++ make py3-pip"
                dir("codegen_typescript") {
                    sh "npm install"
                    // expect it to fail
                    sh "npx tsc || true"
                    sh "node ./dist/codegen.js ../a2lSchema.json"
                    sh "npx tsc"
                    sh "node ./dist/sample.js ../freeTestA2l.minified.json"
                }
            }
            
            post {
                success {
                    archiveArtifacts 'codegen_typescript/src/a2l.d.ts'
                }
            }
        }
    }
}
