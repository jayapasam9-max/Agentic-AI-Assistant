output "cluster_name" {
  value = module.eks.cluster_name
}

output "cluster_endpoint" {
  value = module.eks.cluster_endpoint
}

output "kubeconfig_command" {
  description = "Run this to configure kubectl"
  value       = "aws eks update-kubeconfig --region ${var.region} --name ${module.eks.cluster_name}"
}

output "rds_endpoint" {
  value     = module.rds.endpoint
  sensitive = true
}

output "msk_bootstrap_brokers" {
  value     = module.msk.bootstrap_brokers
  sensitive = true
}

output "ecr_backend_url" {
  value = module.ecr.repository_urls["code-review-agent-backend"]
}

output "ecr_frontend_url" {
  value = module.ecr.repository_urls["code-review-agent-frontend"]
}
