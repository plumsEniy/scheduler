<#if catalogList?has_content>
catalogs:
  catalogSpec:
  <#list catalogList as item>
  - name: ${item.fileName}
    content:
    <#list item.configItemList as configItem>
      ${configItem.configItemKey}: "${configItem.configItemValue}"
    </#list>
  </#list>
</#if>
imageDetails:
  name: ${imageName}
  prestoPath: "/data/app/trino/etc"
  start: "${start}"
  shutdown : "/data/app/trino/shutdown.sh"
<#if capacity?has_content>
capacity: ${capacity}
</#if>
volumes:
  - name: keytabs
    mountPath: "/etc/security/keytabs"
    hostPath:
      path: "/etc/security/keytabs"
  - name: krb5
    mountPath: "/etc/krb5.conf"
    hostPath:
      path: "/etc/krb5.conf"
coordinator:
  memoryLimit: ${(coordinator.memoryLimit)}
  cpuLimit: ${(coordinator.cpuLimit)}
  count: ${(coordinator.count)!"1"}
  localDiskEnabled: ${(coordinator.localDiskEnabled)?c}
<#if (coordinator.additionalJVMConfigList)?has_content >
  additionalJVMConfig: |
  <#list coordinator.additionalJVMConfigList as item>
    ${item}
  </#list>
</#if>
<#if (coordinator.additionalPropMap)?has_content>
  additionalProps:
    <#list coordinator.additionalPropMap?keys as key>
    ${key}: ${coordinator.additionalPropMap[key]}
    </#list>
</#if>

<#if (resource)??>
resource:
  memoryLimit: ${(resource.memoryLimit)!"~"}
  cpuLimit: ${(resource.cpuLimit)!"~"}
  count: ${(resource.count)!"~"}
  localDiskEnabled: ${(resource.localDiskEnabled)?c}
<#if (resource.additionalJVMConfigList)?has_content >
  additionalJVMConfig: |
    <#list resource.additionalJVMConfigList as item>
    ${item}
    </#list>
</#if>
<#if (resource.additionalPropMap)?has_content>
  additionalProps:
    <#list resource.additionalPropMap?keys as key>
    ${key}: ${resource.additionalPropMap[key]}
    </#list>
</#if>
</#if>
worker:
  memoryLimit: ${(worker.memoryLimit)!"~"}
  cpuLimit: ${(worker.cpuLimit)!"~"}
  count: ${(worker.count)!"~"}
  localDiskEnabled: ${(worker.localDiskEnabled)?c}
<#if (worker.additionalJVMConfigList)?has_content >
  additionalJVMConfig: |
    <#list worker.additionalJVMConfigList as item>
    ${item}
    </#list>
</#if>
<#if (worker.additionalPropMap)?has_content>
  additionalProps:
    <#list worker.additionalPropMap?keys as key>
    ${key}: ${worker.additionalPropMap[key]}
    </#list>
  </#if>
<#if additionalPrestoPropsList?has_content >
additionalPrestoPropFiles:
  <#list additionalPrestoPropsList as item>
  ${item.key}: |
  <#list item.valueList as value>
    ${value}
  </#list>
  </#list>
</#if>
