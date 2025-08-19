package com.bilibili.cluster.scheduler.api.service.bmr.config;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ComponentConfigVersionEntity;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigData;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigDetailData;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.*;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.req.*;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.resp.*;
import com.bilibili.cluster.scheduler.common.dto.yarn.req.UpdateFileIpListReq;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.ConfigFileTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.ConfigVersionType;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.FileOperateType;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.FileType;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import com.bilibili.cluster.scheduler.common.utils.BaseRespUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @Date: 2024/5/13 11:54
 * @Author: nizhiqiang
 */

@Slf4j
@Service
public class BmrConfigServiceImpl implements BmrConfigService {

    @Value("${bmr.base-url:http://uat-cloud-bm.bilibili.co}")
    private String BASE_URL = "http://uat-cloud-bm.bilibili.co";

    @Override
    public Boolean updateFileList(List<UpdateFileDto> fileDtoList) {
        String url = UrlBuilder.of(BASE_URL)
                .addPath("/bmr/config/service/api/special/file/batch/update/file/for/node/operate")
                .build();
        log.info("update file list req is {},url is {}", JSONUtil.toJsonStr(fileDtoList), url);
        String respStr = HttpRequest.post(url).header(cn.hutool.http.Header.CONTENT_TYPE, "application/json")
                .timeout(20_000)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .body(JSONUtil.toJsonStr(fileDtoList))
                .execute().body();
        log.info("update file list resp is {}", JSONUtil.toJsonStr(respStr));
        UpdateFileListResp resp = JSONUtil.toBean(respStr, UpdateFileListResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return true;
    }

    @Override
    public Boolean updateFileList(List<Long> componentIdList, String fileName, FileType fileType, List<String> list, Map<String, String> map, FileOperateType operateType) {
        List<UpdateFileDto> updateFileDtoList = new ArrayList<>();
        for (Long componentId : componentIdList) {
            UpdateFileDto updateFileDto = new UpdateFileDto();
            updateFileDto.setComponentId(componentId);
            updateFileDto.setFileName(fileName);
            updateFileDto.setFileType(fileType);
            updateFileDto.setIpList(list);
            updateFileDto.setMap(map);
            updateFileDto.setOperateType(operateType);
            updateFileDtoList.add(updateFileDto);
        }
        return updateFileList(updateFileDtoList);
    }

    @Override
    public FileDownloadData queryDownloadInfoByComponentId(long componentId, String fileName) {
        String url = UrlBuilder.of(BASE_URL)
                .addPath("/bmr/config/service/api/special/file/query/file/download/url")
                .addQuery("componentId", String.valueOf(componentId))
                .addQuery("fileName", fileName)
                .build();
        String respStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();
        QueryDownloadInfoByComponentIdResp resp = JSONUtil.toBean(respStr, QueryDownloadInfoByComponentIdResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj();
    }

    @Override
    public ConfigDetailData queryConfigDetailById(long configId) {
        String url = UrlBuilder.of(BASE_URL)
                .addPath("/bmr/config/service/api/config/version/get/version/info")
                .addQuery("id", String.valueOf(configId))
                .build();
        String respStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();
        QueryConfigResp resp = JSONUtil.toBean(respStr, QueryConfigResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj();
    }

    @Override
    public List<ConfigGroupDo> queryConfigGroupInfoById(long configId) {

        String url = UrlBuilder.of(BASE_URL)
                .addPath("/bmr/config/service/api/config/version/query-version-group-and-file")
                .addQuery("versionId", String.valueOf(configId))
                .build();
        String respStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();
        QueryConfigGroupInfoByIdResp resp = JSONUtil.toBean(respStr, QueryConfigGroupInfoByIdResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj().getLogicGroups();
    }

    @Override
    public ConfigData queryItemListAndData(Long configVersionId, Long componentId, String fileName
            , ConfigFileTypeEnum configFileType, ConfigVersionType configVersionType) {
        QueryConfigFileReq req = new QueryConfigFileReq();
        req.setVersionId(configVersionId);
        req.setComponentId(componentId);
        req.setFileName(fileName);
        req.setFileTypeEnum(configFileType);
        req.setVersionType(configVersionType);

        log.info("query item, component id is {},file name is {}", configVersionId, fileName);
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/bmr/config/service/api/config/file/query/file/general")
                .build();

        String respStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .body(JSONUtil.toJsonStr(req))
                .execute().body();

        log.info("query item, resp {}", respStr);
        QueryConfigFileResp resp = JSONUtil.toBean(respStr, QueryConfigFileResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj();
    }

    @Override
    public List<ConfigGroupRelationEntity> queryConfigGroupRelation(long configVersionId) {

        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/bmr/config/service/api/config/group/list/by/version-id")
                .addQuery("versionId", String.valueOf(configVersionId))
                .build();

        String respStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();

        QueryConfigGroupRelationResp resp = JSONUtil.toBean(respStr, QueryConfigGroupRelationResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj();
    }

    @Override
    public List<ConfigFileEntity> queryFileListByGroupId(long configGroupId) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/bmr/config/service/api/config/file/list")
                .addQuery("configGroupId", String.valueOf(configGroupId))
                .build();
        String respStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();
        QueryFileListByGroupIdResp resp = JSONUtil.toBean(respStr, QueryFileListByGroupIdResp.class);
        return resp.getObj();
    }

    @Override
    public ConfigDetailData queryConfigDetailByComponentId(long componentId) {
        String url = UrlBuilder.of(BASE_URL)
                .addPath("/bmr/config/service/api/config/version/get/version/info")
                .addQuery("componentId", String.valueOf(componentId))
                .addQuery("id", String.valueOf(0))
                .build();
        String respStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();
        QueryConfigResp resp = JSONUtil.toBean(respStr, QueryConfigResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj();
    }

    @Override
    public List<ComponentConfigVersionEntity> queryComponentConfigVersionList(long componentId, String versionName, int pageSize, int pageNum) {
        String url = UrlBuilder.of(BASE_URL)
                .addPath("/bmr/config/service/api/config/version/list/page")
                .build();
        QueryPageVersionListReq req = new QueryPageVersionListReq();
        req.setComponentId(componentId);
        req.setVersionName(versionName);
        req.setPageNum(1);
        req.setPageSize(1);
        String respStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .body(JSONUtil.toJsonStr(req))
                .execute().body();

        log.info("query component version page resp is {}", respStr);
        QueryPageVersionListResp resp = JSONUtil.toBean(respStr, QueryPageVersionListResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj().getRecords();
    }

    @Override
    public void saveFile(SaveFileReq req) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/bmr/config/service/api/config/file/save/file/by/component-id")
                .build();

        log.info("save file req is {}", JSONUtil.toJsonStr(req));

        String respStr = HttpRequest.post(url)
                .body(JSONUtil.toJsonStr(req))
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute()
                .body();

        BaseMsgResp resp = JSONUtil.toBean(respStr, BaseMsgResp.class);
        BaseRespUtil.checkMsgResp(resp);
    }

    @Override
    public void updateSpecialKeyValueFile(UpdateSpecialKeyValueFileReq req) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/bmr/config/service/api/config/file/update/file/context")
                .build();
        String reqJson = JSONUtil.toJsonStr(req);

        log.info("update special key value file url is {}, req is {}", url, reqJson);

        String respStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .body(reqJson)
                .execute()
                .body();

        log.info("update special key value file resp is {}", respStr);
        BaseMsgResp resp = JSONUtil.toBean(respStr, BaseMsgResp.class);
        BaseRespUtil.checkMsgResp(resp);
    }

    @Override
    public long queryDefaultConfigVersionIdByComponentId(long componentId) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/bmr/config/service/api/config/version/get/stable/version/by/componentId")
                .addQuery("componentId", String.valueOf(componentId))
                .build();

        String respStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();

        QueryDefaultConfigResp resp = JSONUtil.toBean(respStr, QueryDefaultConfigResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj().getId();
    }

    @Override
    public Boolean updateFileIpList(long componentId, String fileName, FileOperateType operateType, List<String> ipList) {
        String path = "/bmr/config/service/api/special/file/update/file/for/node/operate";
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath(path)
                .build();
        UpdateFileIpListReq req = new UpdateFileIpListReq(componentId, fileName, operateType, ipList);
        String body = JSONUtil.toJsonStr(req);
        log.info("rm conf updateFileIpList url {}, body is {}", url, body);

        String respStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .body(body)
                .execute()
                .body();

        BaseMsgResp resp = JSONUtil.toBean(respStr, BaseMsgResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return true;
    }

    @Override
    public ConfigGroupRelationEntity queryDefaultGroupRelation(long configVersionId) {

        List<ConfigGroupRelationEntity> configGroupRelationEntityList = queryConfigGroupRelation(configVersionId);
        ConfigGroupRelationEntity configGroupRelation = null;
        for (ConfigGroupRelationEntity configGroupRelationEntity : configGroupRelationEntityList) {
            String groupName = configGroupRelationEntity.getResourceGroupName();
            if (Constants.DEFAULT_GROUP_NAME.equals(groupName)) {
                configGroupRelation = configGroupRelationEntity;
                break;
            }
        }
        return configGroupRelation;
    }

    @Override
    public void coverSpecialFileContext(long componentId, String fileName, String fileContext) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/bmr/config/service/api/special/file/cover/file/string/context")
                .build();

        CoverFileContextReq req = new CoverFileContextReq(componentId, fileName, fileContext);
        String body = JSONUtil.toJsonStr(req);
        log.info("cover special file url {}, body is {}", url, body);

        String respStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .body(body)
                .execute()
                .body();

        BaseMsgResp resp = JSONUtil.toBean(respStr, BaseMsgResp.class);
        BaseRespUtil.checkMsgResp(resp);
    }

}
