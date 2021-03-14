terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "3.25.0"
    }
  }
}

provider "aws" {
  profile = "devops-rnd"
  region  = "eu-west-1"
}

variable "project-name" {
  default = "iot-messaging"
}

# IAM stuff

resource "aws_iam_role" "iot-disconnect-alert-role" {
  name = "iot-disconnect-alert-role"

  tags = {
    Project = var.project-name
  }

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": ["iot.amazonaws.com", "lambda.amazonaws.com"]
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF
}

resource "aws_iam_policy" "iot-disconnect-alert-policy" {
  name        = "iot-disconnect-alert-policy"
  description = "Allows sending a message to a queue and writing data to a DynamoDB table"

  policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": ["sqs:GetQueueAttributes", "sqs:DeleteMessage", "sqs:ReceiveMessage", "sqs:SendMessage"],
            "Resource": "${aws_sqs_queue.delayed-queue.arn}"
        },
        {
            "Effect": "Allow",
            "Action": ["dynamodb:PutItem", "dynamodb:UpdateItem", "dynamodb:GetItem"],
            "Resource": "${aws_dynamodb_table.connect-state-table.arn}"
        },      
        {
            "Effect": "Allow",
            "Action": "lambda:InvokeFunction",
            "Resource": "${aws_lambda_function.connect-state-writer-lambda.arn}"
        }       
    ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "attach" {
  role       = aws_iam_role.iot-disconnect-alert-role.name
  policy_arn = aws_iam_policy.iot-disconnect-alert-policy.arn
}

resource "aws_iam_policy" "lambda_logging" {
  name        = "lambda_logging"
  path        = "/"
  description = "IAM policy for logging from a lambda"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*",
      "Effect": "Allow"
    }
  ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "lambda_logs" {
  role       = aws_iam_role.iot-disconnect-alert-role.name
  policy_arn = aws_iam_policy.lambda_logging.arn
}

# SQS

resource "aws_sqs_queue" "delayed-queue" { 
  name                      = "iot-client-disconnect-alert-queue"
  delay_seconds             = 10
  message_retention_seconds = 60
  receive_wait_time_seconds = 10  
	
  tags = {
    Project  = var.project-name
  }	
}

# IoT

resource "aws_iot_topic_rule" "connect-rule" {
  name        = "connect_state_change"
  description = "Sends a message to a lambda when a client connects or disconnects"  
  enabled     = true
  sql         = "SELECT * FROM '$aws/events/presence/+'"
  sql_version = "2016-03-23"

  lambda {
    function_arn = aws_lambda_function.connect-state-writer-lambda.arn
  }

  tags = {
    Project  = var.project-name
  }
}

resource "aws_iot_topic_rule" "disconnect-rule" {
  name        = "client_disconnect"
  description = "Puts a disconnect in the delayed queue"
  enabled     = true
  sql         = "SELECT * FROM '$aws/events/presence/disconnected/'" 
  sql_version = "2016-03-23"

  sqs {
    queue_url  = aws_sqs_queue.delayed-queue.id
    role_arn   = aws_iam_role.iot-disconnect-alert-role.arn
    use_base64 = false
  }

  tags = {
    Project  = var.project-name
  }
}

# DynamoDB

resource "aws_dynamodb_table" "connect-state-table" {
  name             = "iot-client-connect-state"
  billing_mode     = "PAY_PER_REQUEST"
  hash_key         = "clientId"
  ttl {
    attribute_name = "expiration"
    enabled        = true
  }

  attribute {
    name = "clientId"
    type = "S"
  }

  tags = {
    Project  = var.project-name
  }
}

# Lambda

resource "aws_lambda_function" "connect-state-writer-lambda" {
  filename      = "IotClientDisconnectAlertLambda-1.0-SNAPSHOT.zip" 
  source_code_hash = filebase64sha256("IotClientDisconnectAlertLambda-1.0-SNAPSHOT.zip")
  function_name = "iot-connect-state-writer-lambda"
  description   = "Writes IoT dis/connect events to DynamoDB"
  role          = aws_iam_role.iot-disconnect-alert-role.arn
  handler       = "com.sysaid.disconnectcheck.ConnectStateChangedHandler"
  runtime       = "java8"
  timeout       = 30
  memory_size   = 2048

  environment {
    variables = {
      TABLE_NAME = aws_dynamodb_table.connect-state-table.id
    }
  }

  tags = {
    Project  = var.project-name
  }
}

resource "aws_lambda_permission" "allow_cloudwatch" {
  statement_id  = "AllowExecutionFromIot"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.connect-state-writer-lambda.function_name
  principal     = "iot.amazonaws.com"
  source_arn    = aws_iot_topic_rule.connect-rule.arn
}

resource "aws_lambda_function" "connect-state-check-lambda" {
  filename      = "IotClientDisconnectAlertLambda-1.0-SNAPSHOT.zip"
  source_code_hash = filebase64sha256("IotClientDisconnectAlertLambda-1.0-SNAPSHOT.zip")
  function_name = "iot-connect-state-check-lambda"
  description   = "Checks the connect state of an IoT client"
  role          = aws_iam_role.iot-disconnect-alert-role.arn
  handler       = "com.sysaid.disconnectcheck.DisconnectHandler"
  runtime       = "java8"
  timeout       = 30
  memory_size   = 2048

  environment {
    variables = {
      TABLE_NAME = aws_dynamodb_table.connect-state-table.id
    }
  }

  tags = {
    Project  = var.project-name
  }
}


resource "aws_lambda_event_source_mapping" "event_source_mapping" {
  event_source_arn = aws_sqs_queue.delayed-queue.arn
  enabled          = true
  function_name    = aws_lambda_function.connect-state-check-lambda.arn
  batch_size       = 1
}
