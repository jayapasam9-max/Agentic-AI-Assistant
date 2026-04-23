variable "cluster_name"     { type = string }
variable "cluster_endpoint" { type = string }

# Installs Prometheus + Grafana + Alertmanager + node exporters + kube-state-metrics
# in one shot. Default storage is 50Gi for Prometheus retention (15 days).
resource "helm_release" "kube_prometheus_stack" {
  name             = "kube-prometheus-stack"
  repository       = "https://prometheus-community.github.io/helm-charts"
  chart            = "kube-prometheus-stack"
  namespace        = "monitoring"
  create_namespace = true
  version          = "62.6.0"

  values = [
    yamlencode({
      prometheus = {
        prometheusSpec = {
          retention = "15d"
          storageSpec = {
            volumeClaimTemplate = {
              spec = {
                storageClassName = "gp3"
                accessModes      = ["ReadWriteOnce"]
                resources        = { requests = { storage = "50Gi" } }
              }
            }
          }
          # Auto-discover ServiceMonitors from any namespace
          serviceMonitorSelectorNilUsesHelmValues = false
          podMonitorSelectorNilUsesHelmValues     = false
        }
      }
      grafana = {
        adminPassword = "CHANGE_ME_AFTER_INSTALL"
        persistence = {
          enabled          = true
          storageClassName = "gp3"
          size             = "10Gi"
        }
        # Dashboards can be provisioned via ConfigMaps labeled grafana_dashboard=1
        sidecar = {
          dashboards = { enabled = true }
          datasources = { enabled = true }
        }
      }
      alertmanager = {
        alertmanagerSpec = {
          storage = {
            volumeClaimTemplate = {
              spec = {
                storageClassName = "gp3"
                accessModes      = ["ReadWriteOnce"]
                resources        = { requests = { storage = "10Gi" } }
              }
            }
          }
        }
      }
    })
  ]
}

# ServiceMonitor that scrapes /actuator/prometheus from the backend pods.
# The Spring Boot service must be labeled app=code-review-agent-backend
# and expose port 8080 named "http".
resource "kubernetes_manifest" "backend_service_monitor" {
  manifest = {
    apiVersion = "monitoring.coreos.com/v1"
    kind       = "ServiceMonitor"
    metadata = {
      name      = "code-review-agent-backend"
      namespace = "monitoring"
      labels    = { release = "kube-prometheus-stack" }
    }
    spec = {
      selector = {
        matchLabels = { app = "code-review-agent-backend" }
      }
      namespaceSelector = { any = true }
      endpoints = [{
        port     = "http"
        path     = "/actuator/prometheus"
        interval = "15s"
      }]
    }
  }

  depends_on = [helm_release.kube_prometheus_stack]
}
