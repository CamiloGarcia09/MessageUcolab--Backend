targetScope = 'resourceGroup'

@description('Región de Azure utilizada por el despliegue.')
param location string = resourceGroup().location

@description('Prefijo corto en minúsculas utilizado para los nombres de los recursos.')
@minLength(3)
@maxLength(18)
param namePrefix string

@description('Nombre globalmente único de Azure Container Registry.')
param acrName string

@description('Suscripción donde se encuentra el Key Vault existente. De forma predeterminada, se utiliza la suscripción actual.')
param keyVaultSubscriptionId string = subscription().subscriptionId

@description('Grupo de recursos que contiene el Key Vault existente.')
param keyVaultResourceGroupName string

@description('Nombre del Key Vault existente.')
param keyVaultName string

@description('Establecer en false cuando el Key Vault existente utilice directivas de acceso en lugar de Azure RBAC.')
param assignKeyVaultRbacRole bool = true

@description('Tamaño de la máquina virtual Linux. Standard_B2s es la recomendación inicial para la demostración.')
param vmSize string = 'Standard_B2s'

@description('Nombre del usuario administrador. La autenticación mediante contraseña está deshabilitada.')
param adminUsername string = 'azureuser'

@description('Clave SSH pública utilizada únicamente para mantenimiento controlado y despliegues desde el pipeline.')
param adminSshPublicKey string

@description('Origen autorizado para acceder a la puerta de enlace Kong por el puerto 8000. Utilizar Internet para permitir el acceso público a la demostración.')
param allowedApiSourcePrefix string

@description('Hora diaria de apagado automático en formato HHmm de 24 horas.')
param autoShutdownTime string = '2300'

@description('Zona horaria de Windows compatible con IANA utilizada por el apagado automático de Azure.')
param autoShutdownTimeZone string = 'SA Pacific Standard Time'

var vmName = '${namePrefix}-vm'
var vnetName = '${namePrefix}-vnet'
var subnetName = 'application'
var nsgName = '${namePrefix}-nsg'
var publicIpName = '${namePrefix}-pip'
var nicName = '${namePrefix}-nic'
var acrPullRoleDefinitionId = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '7f951dda-4ed3-4680-a7ca-43fe172d538d')

resource acr 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: acrName
  location: location
  sku: {
    name: 'Standard'
  }
  properties: {
    adminUserEnabled: false
    publicNetworkAccess: 'Enabled'
    policies: {
      quarantinePolicy: {
        status: 'disabled'
      }
      retentionPolicy: {
        days: 7
        status: 'disabled'
      }
      trustPolicy: {
        type: 'Notary'
        status: 'disabled'
      }
    }
  }
}

resource nsg 'Microsoft.Network/networkSecurityGroups@2024-05-01' = {
  name: nsgName
  location: location
  properties: {
    securityRules: [
      {
        name: 'AllowApplicationDemo'
        properties: {
          priority: 200
          access: 'Allow'
          direction: 'Inbound'
          protocol: 'Tcp'
          sourcePortRange: '*'
          destinationPortRange: '8000'
          sourceAddressPrefix: allowedApiSourcePrefix
          destinationAddressPrefix: '*'
        }
      }
    ]
  }
}

resource vnet 'Microsoft.Network/virtualNetworks@2024-05-01' = {
  name: vnetName
  location: location
  properties: {
    addressSpace: {
      addressPrefixes: [
        '10.20.0.0/16'
      ]
    }
    subnets: [
      {
        name: subnetName
        properties: {
          addressPrefix: '10.20.1.0/24'
          networkSecurityGroup: {
            id: nsg.id
          }
        }
      }
    ]
  }
}

resource publicIp 'Microsoft.Network/publicIPAddresses@2024-05-01' = {
  name: publicIpName
  location: location
  sku: {
    name: 'Standard'
  }
  properties: {
    publicIPAllocationMethod: 'Static'
    publicIPAddressVersion: 'IPv4'
    idleTimeoutInMinutes: 15
  }
}

resource nic 'Microsoft.Network/networkInterfaces@2024-05-01' = {
  name: nicName
  location: location
  properties: {
    ipConfigurations: [
      {
        name: 'ipconfig1'
        properties: {
          privateIPAllocationMethod: 'Dynamic'
          subnet: {
            id: resourceId('Microsoft.Network/virtualNetworks/subnets', vnet.name, subnetName)
          }
          publicIPAddress: {
            id: publicIp.id
          }
        }
      }
    ]
  }
}

resource vm 'Microsoft.Compute/virtualMachines@2024-07-01' = {
  name: vmName
  location: location
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    hardwareProfile: {
      vmSize: vmSize
    }
    storageProfile: {
      imageReference: {
        publisher: 'Canonical'
        offer: 'ubuntu-24_04-lts'
        sku: 'server'
        version: 'latest'
      }
      osDisk: {
        createOption: 'FromImage'
        diskSizeGB: 64
        managedDisk: {
          storageAccountType: 'StandardSSD_LRS'
        }
      }
    }
    osProfile: {
      computerName: vmName
      adminUsername: adminUsername
      customData: base64(replace(loadTextContent('cloud-init.yml'), '__ADMIN_USERNAME__', adminUsername))
      linuxConfiguration: {
        disablePasswordAuthentication: true
        ssh: {
          publicKeys: [
            {
              path: '/home/${adminUsername}/.ssh/authorized_keys'
              keyData: adminSshPublicKey
            }
          ]
        }
      }
    }
    networkProfile: {
      networkInterfaces: [
        {
          id: nic.id
        }
      ]
    }
    diagnosticsProfile: {
      bootDiagnostics: {
        enabled: true
      }
    }
  }
}

resource acrPullRole 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(acr.id, vm.id, acrPullRoleDefinitionId)
  scope: acr
  properties: {
    principalId: vm.identity.principalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: acrPullRoleDefinitionId
  }
}

module keyVaultAccess 'key-vault-access.bicep' = if (assignKeyVaultRbacRole) {
  name: '${namePrefix}-key-vault-access'
  scope: resourceGroup(keyVaultSubscriptionId, keyVaultResourceGroupName)
  params: {
    keyVaultName: keyVaultName
    principalId: vm.identity.principalId
  }
}

resource shutdownSchedule 'Microsoft.DevTestLab/schedules@2018-09-15' = {
  name: 'shutdown-computevm-${vm.name}'
  location: location
  properties: {
    status: 'Enabled'
    taskType: 'ComputeVmShutdownTask'
    dailyRecurrence: {
      time: autoShutdownTime
    }
    timeZoneId: autoShutdownTimeZone
    targetResourceId: vm.id
    notificationSettings: {
      status: 'Disabled'
      timeInMinutes: 30
    }
  }
}

output acrLoginServer string = acr.properties.loginServer
output acrNameOutput string = acr.name
output vmNameOutput string = vm.name
output vmPrincipalId string = vm.identity.principalId
output publicIpAddress string = publicIp.properties.ipAddress
output nsgNameOutput string = nsg.name
