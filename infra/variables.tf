variable "project" {
  description = "Project identifier, used as a prefix for all resources"
  type        = string
  default     = "codereview"
}

variable "environment" {
  description = "Deployment environment (dev, staging, prod)"
  type        = string
  default     = "dev"

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment must be one of: dev, staging, prod"
  }
}

variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "domain_name" {
  description = "Base domain for the ALB (e.g., codereview.example.com). Leave empty to skip DNS/ACM setup."
  type        = string
  default     = ""
}
