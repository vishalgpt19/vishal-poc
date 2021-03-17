pipeline 
{
    environment 
    {
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
                sh '''
                            echo "**********************************$(date "+%m%d%Y %T") : Docker Login to ECR**********************************"
                            echo ${BUILD_NUMBER}
                '''
            }
        }

        stage('Approval for destroying terraform resources and cleaning workspace')
        {
			steps
			{
                script
                { 
                    def mailRecipients = 'vishalgpt19@gmail.com'
                    def approversList = 'vishal.gupta,admin'
                    def jobName = currentBuild.fullDisplayName
                    def userAborted = false
                    emailext body: '''
                    Please go to console output of ${BUILD_URL}input to approve or Reject.<br>
                    ''',    
                    mimeType: 'text/html',
                    subject: "[Jenkins] ${jobName} Build Approval Request",
                    to: "${mailRecipients}",
                    replyTo: "${mailRecipients}",
                    recipientProviders: [[$class: 'CulpritsRecipientProvider']]

                    echo "Email Triggered for approval of pipeline"
                    try 
                    { 
                        userInput = input submitter: "${approversList}", message: 'Do you approve?'
                    } 
                    catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) 
                    {
                        cause = e.causes.get(0)
                        echo "Aborted by " + cause.getUser().toString()
                        userAborted = true
                        echo "SYSTEM aborted, but looks like timeout period didn't complete. Aborting."
                    }
                    if (userAborted) 
                    {
                        currentBuild.result = 'ABORTED'
                        echo "Build aborted by user/Unauthorized Access"
                    } 
                    else 
                    {
                        // some block
                        sh '''
                        echo "**********************************$(date "+%m%d%Y %T") : Yes is selected, hence destroying the terraform resources**********************************"
                        '''
                    }
                }	
			}
        }
    }
}

