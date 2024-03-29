pipeline 
{
    environment 
    {
        AWS_DEFAULT_REGION = "us-east-2"
        AWS_ACCOUNT_ID = "245616658709"
        JAVA_IMAGE_REPO_NAME = "javaimage"
        PYTHON_IMAGE_REPO_NAME = "pythonimage"
        BUILD_NUMBER = "${env.BUILD_NUMBER}"
    }

    agent any
    stages 
    {
        stage('Hello') 
        {
            steps 
            {
                echo 'Hello World'
            }
        }
        
        stage('Git Clone') 
        {
            steps 
            {         
                git credentialsId: 'cf526297-6cc4-44d1-ba6e-340e324c6c80', url: 'https://github.com/vishalgpt19/vishal-poc'
            }
        }

        stage('Checking Files') 
        {
            steps 
            {
                sh '''
                            echo "**********************************$(date "+%m%d%Y %T") : Checking the Files Package**********************************"
                            echo ${WORKSPACE}
                            pwd
                            ls
                '''  
            }
        }

        stage('Docker Login') 
        {
            steps 
            {
                sh '''
                            echo "**********************************$(date "+%m%d%Y %T") : Docker Login to ECR**********************************"
                            echo ${WORKSPACE}
                            echo Logging in to Amazon ECR...
                            aws ecr get-login-password --region $AWS_DEFAULT_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com               
                '''  
            }
        }

        stage('Docker Build and Push Java App') 
        {
            steps 
            {
                sh '''
                            echo "**********************************$(date "+%m%d%Y %T") : Building JAVA Docker Image**********************************"
                            cd java-app
                            docker build -t $JAVA_IMAGE_REPO_NAME:$BUILD_NUMBER .
                            docker tag $JAVA_IMAGE_REPO_NAME:$BUILD_NUMBER $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$JAVA_IMAGE_REPO_NAME:$BUILD_NUMBER
                            docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$JAVA_IMAGE_REPO_NAME:$BUILD_NUMBER                                                  
                '''  
            }
        }

        stage('Docker Build and Push Python App') 
        {
            steps 
            {
                sh '''
                            echo "**********************************$(date "+%m%d%Y %T") : Building Python Docker Image**********************************"
                            cd python-app
                            docker build -t $PYTHON_IMAGE_REPO_NAME:$BUILD_NUMBER .
                            docker tag $PYTHON_IMAGE_REPO_NAME:$BUILD_NUMBER $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$PYTHON_IMAGE_REPO_NAME:$BUILD_NUMBER
                            docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$PYTHON_IMAGE_REPO_NAME:$BUILD_NUMBER 
                '''  
            }
        }

        stage('Approval')
        {
		    input
            {
                message "Do you want to proceed for production deployment?"
            }
            steps
            {
                sh '''
                    echo "You clicked on Yes."
                '''
            }
        }

        stage('Deploy the Updated Code') 
        {
            steps 
            {
                sh '''
                        echo "**********************************$(date "+%m%d%Y %T") : Deploying the Docker Images to Kubernetes Cluster**********************************"
                        #sed -i "s/{{BUILD_NUMBER}}/$BUILD_NUMBER/g" ./kubernetes-files/java-deployment.yaml
                        #sed -i "s/{{BUILD_NUMBER}}/$BUILD_NUMBER/g" ./kubernetes-files/python-deployment.yaml
                        
                        sudo -u ubuntu -H sh -c "kubectl set image deployment/hello-java hello-java=245616658709.dkr.ecr.us-east-2.amazonaws.com/javaimage:$BUILD_NUMBER --record"
                        
                        sudo -u ubuntu -H sh -c "kubectl set image deployment/hello-python hello-python=245616658709.dkr.ecr.us-east-2.amazonaws.com/pythonimage:$BUILD_NUMBER --record" 

                        sudo -u ubuntu -H sh -c "kubectl get all" 
                '''  
            }
        }

    }

    post
    {
        always
        {
            // make sure that the Docker image is removed
            sh '''
            docker rmi -f $PYTHON_IMAGE_REPO_NAME:$BUILD_NUMBER
            docker rmi -f $JAVA_IMAGE_REPO_NAME:$BUILD_NUMBER
            docker rmi -f $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$PYTHON_IMAGE_REPO_NAME:$BUILD_NUMBER
            docker rmi -f $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$JAVA_IMAGE_REPO_NAME:$BUILD_NUMBER
            '''
        }
    }
}

