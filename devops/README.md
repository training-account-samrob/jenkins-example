# DevOps

## useful links (will all be dead once this project has been assessed because the VMs will be decomissioned)

- jenkins: http://145.241.247.39:8080
- argoCD: http://79.72.68.49:31706
- Grafana: http://79.72.68.49:32000/d/eecba7a9-cad4-42ff-9f40-ef5096669bbb/app-services
- React App: http://79.72.68.49:30124


### Jenkins uses a seed job set up
- All jenkins code is within the devops/jenkins/jobs path
- Jenkins runs on an oracle VM, in a container, thanks to docker-compose

### Kubernetes cluster
- This is running in a K3s cluster on an oracle VM

### ArgoCD manages the applications
- Application CRDs defined within the devops/kubernetes/gitops/apps path 
- manifests are in devops/kubernetes/gitops/manifests path
- prometheus/grafana is not managed by argocd and was installed imperatively with helm

## healthcheck script
- A healthcheck bash script is used during CI. This can be found at devops/scripts path