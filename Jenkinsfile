pipeline {
    agent any

    tools {
        maven 'maven-3.9.14'
    }

    stages {
        stage('1. Build Artifacts') {
            steps {
                echo 'Dang thuc hien Build Maven cho toan bo du an...'
                sh 'mvn clean install -DskipTests'
            }
        }

        stage('2. Build Docker Images') {
            steps {
                echo 'Dang dong goi cac Services thanh Docker Image...'
                sh 'docker-compose build'
            }
        }

        stage('3. Deploy and Run System') {
            steps {
                echo 'Dang khoi chay toan bo he thong...'
                sh 'docker-compose down'
                sh 'docker-compose up -d'
            }
        }
    }

    post {
        success {
            echo 'Chuc mung! Pipeline da chay thanh cong ruc ro.'
        }
        failure {
            echo 'Pipeline that bai! Kiem tra lai log nhe.'
        }
    }
}