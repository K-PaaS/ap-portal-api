package org.openpaas.portal.api.service;

import org.cloudfoundry.client.lib.org.codehaus.jackson.map.ObjectMapper;
import org.cloudfoundry.client.lib.org.codehaus.jackson.type.TypeReference;
import org.cloudfoundry.client.v2.OrderDirection;
import org.cloudfoundry.client.v2.applications.*;
import org.cloudfoundry.client.v2.applications.TerminateApplicationInstanceRequest;
import org.cloudfoundry.client.v2.applications.UpdateApplicationRequest;
import org.cloudfoundry.client.v2.events.ListEventsRequest;
import org.cloudfoundry.client.v2.events.ListEventsResponse;
import org.cloudfoundry.client.v2.routemappings.CreateRouteMappingRequest;
import org.cloudfoundry.client.v2.routes.CreateRouteRequest;
import org.cloudfoundry.client.v2.routes.CreateRouteResponse;
import org.cloudfoundry.client.v2.routes.DeleteRouteRequest;
import org.cloudfoundry.client.v2.routes.Route;
import org.cloudfoundry.client.v2.servicebindings.CreateServiceBindingRequest;
import org.cloudfoundry.client.v2.servicebindings.DeleteServiceBindingRequest;
import org.cloudfoundry.client.v2.servicebindings.DeleteServiceBindingResponse;
import org.cloudfoundry.client.v2.servicebindings.ServiceBindingResource;
import org.cloudfoundry.client.v2.serviceinstances.ListServiceInstanceServiceBindingsRequest;
import org.cloudfoundry.client.v2.serviceinstances.ListServiceInstanceServiceBindingsResponse;
import org.cloudfoundry.client.v2.userprovidedserviceinstances.GetUserProvidedServiceInstanceRequest;
import org.cloudfoundry.client.v2.userprovidedserviceinstances.GetUserProvidedServiceInstanceResponse;
import org.cloudfoundry.client.v2.userprovidedserviceinstances.ListUserProvidedServiceInstanceServiceBindingsRequest;
import org.cloudfoundry.client.v2.userprovidedserviceinstances.ListUserProvidedServiceInstanceServiceBindingsResponse;
import org.cloudfoundry.client.v3.LifecycleData;
import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.applications.*;
import org.cloudfoundry.client.v3.applications.GetApplicationRequest;
import org.cloudfoundry.client.v3.applications.GetApplicationResponse;
import org.cloudfoundry.client.v3.builds.CreateBuildRequest;
import org.cloudfoundry.client.v3.builds.CreateBuildResponse;
import org.cloudfoundry.client.v3.builds.GetBuildRequest;
import org.cloudfoundry.client.v3.packages.ListPackagesRequest;
import org.cloudfoundry.client.v3.packages.ListPackagesResponse;
import org.cloudfoundry.client.v3.packages.PackageState;
import org.cloudfoundry.client.v3.servicebindings.ListServiceBindingsRequest;
import org.cloudfoundry.client.v3.servicebindings.ListServiceBindingsResponse;
import org.cloudfoundry.client.v3.serviceinstances.*;
import org.cloudfoundry.client.v3.serviceofferings.GetServiceOfferingRequest;
import org.cloudfoundry.client.v3.serviceplans.GetServicePlanRequest;
import org.cloudfoundry.client.v3.serviceplans.GetServicePlanResponse;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.openpaas.portal.api.common.Common;
import org.openpaas.portal.api.common.Constants;
import org.openpaas.portal.api.common.RestTemplateService;
import org.openpaas.portal.api.model.App;
import org.openpaas.portal.api.model.Batch;
import org.openpaas.portal.api.model.ServiceV3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AppServiceV3 extends Common {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppServiceV3.class);

    private final RestTemplateService restTemplateService;

    // build process interval time (sec)
    private final long BUILD_INTERVAL_SECOND = 300;

    public AppServiceV3(RestTemplateService restTemplateService) {
        this.restTemplateService = restTemplateService;
    }

    public SummaryApplicationResponse getAppSummary(String guid, String token) {
        SummaryApplicationResponse summaryApplicationResponse = cloudFoundryClient(tokenProvider(token)).applicationsV2().summary(SummaryApplicationRequest.builder().applicationId(guid).build()).block();
        return summaryApplicationResponse;
    }

    /**
     * 앱 빌드팩을 조회한다.
     *
     * @param guid  the app guid
     * @param token the client
     * @return the app lifecycle
     */
    public LifecycleData getAppBuildpack(String guid, String token) {
        GetApplicationResponse getApplicationResponse = cloudFoundryClient(tokenProvider(token)).applicationsV3().get(GetApplicationRequest.builder().applicationId(guid).build()).block();
        getApplicationResponse.getLifecycle().getData();

        return getApplicationResponse.getLifecycle().getData();
    }

    /**
     * 앱 실시간 상태를 조회한다.
     *
     * @param guid  the app guid
     * @param token the client
     * @return the app stats
     */
    public GetApplicationProcessStatisticsResponse getAppStats(String guid, String token) {
        ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(tokenProvider(token));

        GetApplicationProcessStatisticsResponse applicationStatisticsResponse =
                cloudFoundryClient.applicationsV3().getProcessStatistics(GetApplicationProcessStatisticsRequest.builder().applicationId(guid).type("web").build()).block();

        return applicationStatisticsResponse;
    }

    /**
     * 앱을 변경한다.
     *
     * @param app   the app
     * @param token the client
     * @throws Exception the exception
     */
    public Map renameApp(App app, String token) {
        HashMap result = new HashMap();
        try {
            ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(tokenProvider(token));
            org.cloudfoundry.client.v2.applications.UpdateApplicationResponse response = cloudFoundryClient.applicationsV2().update(org.cloudfoundry.client.v2.applications.UpdateApplicationRequest.builder().applicationId(app.getGuid().toString()).name(app.getNewName()).build()).block();

            LOGGER.info("Update app response :", response);

            result.put("result", true);
            result.put("msg", "You have successfully completed the task.");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("result", false);
            result.put("msg", e.getMessage());
        }

        return result;

    }

    /**
     * 앱을 삭제한다.
     *
     * @param guid the app
     * @throws Exception the exception
     */
    public Map deleteApp(String guid) {
        HashMap result = new HashMap();
        try {
            //앱 삭제
            ReactorCloudFoundryClient reactorCloudFoundryClient = cloudFoundryClient();
            try {
                ListApplicationServiceBindingsResponse listApplicationServiceBindingsResponse = reactorCloudFoundryClient.applicationsV2().listServiceBindings(ListApplicationServiceBindingsRequest.builder().applicationId(guid).build()).block();
                for (ServiceBindingResource resource : listApplicationServiceBindingsResponse.getResources()) {
                    reactorCloudFoundryClient.serviceBindingsV2().delete(DeleteServiceBindingRequest.builder().serviceBindingId(resource.getMetadata().getId()).build()).block();
                }
            } catch (Exception e) {

            }
            List<Route> routes = reactorCloudFoundryClient.applicationsV2().summary(SummaryApplicationRequest.builder().applicationId(guid).build()).block().getRoutes();
            for (Route route : routes) {
                reactorCloudFoundryClient.routes().delete(DeleteRouteRequest.builder().routeId(route.getId()).build()).block();
            }
            //reactorCloudFoundryClient.applicationsV2().delete(DeleteApplicationRequest.builder().applicationId(guid).build()).block();
            reactorCloudFoundryClient.applicationsV3().delete(org.cloudfoundry.client.v3.applications.DeleteApplicationRequest.builder().applicationId(guid).build()).block();
            result.put("result", true);
            result.put("msg", "You have successfully completed the task.");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("result", false);
            result.put("msg", e.getMessage());
        }
        return result;
    }


    /**
     * 앱을 리스테이징한다.
     *
     * @param app   the app
     * @param token the client
     * @the exception
     */
    public Map restageApp(App app, String token) {
        Map resultMap = new HashMap();
        String applicationid = app.getGuid().toString();

        try {
            ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(tokenProvider(token));

            //state가 READY인 가장 최근의 package ID 조회
            ListPackagesResponse listPackagesResponse = cloudFoundryClient.packages().list(ListPackagesRequest.builder().applicationId(app.getGuid().toString()).orderBy("-created_at").state(PackageState.READY).build()).block();
            String recentPackageId = listPackagesResponse.getResources().get(0).getId();

            Thread th = new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                createBuild(applicationid, recentPackageId, cloudFoundryClient);
                                startApp(applicationid, token);
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                    );
            th.start();

            resultMap.put("result", true);
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", false);
            resultMap.put("msg", e);
        }

        return resultMap;
    }



    /**
     * 빌드를 생성한다.
     *
     * @param applicationid             String
     * @param packageId                 String
     * @param reactorCloudFoundryClient ReactorCloudFoundryClient
     * @return Map(자바클래스)
     * @throws Exception Exception(자바클래스)
     */
    public Map<String, Object> createBuild(String applicationid, String packageId, ReactorCloudFoundryClient reactorCloudFoundryClient) throws Exception {
        try {
            // 빌드 생성
            CreateBuildResponse buildResponse = reactorCloudFoundryClient.builds().create(CreateBuildRequest.builder().getPackage(Relationship.builder().id(packageId).build()).build()).block();

            //현재 시각
            long start = System.currentTimeMillis();

            //종료 시각
            long end = start + BUILD_INTERVAL_SECOND *1000;

            // 빌드 확인 중 = STAGED
            while(true){
                if( reactorCloudFoundryClient.builds().get(GetBuildRequest.builder().buildId(buildResponse.getId()).build()).block().getState().getValue().equals("STAGED") ) {
                    break;
                }
                if ( System.currentTimeMillis() > end ){
                    throw new Exception("App Build Time Over");
                }
                Thread.sleep(1000);
            }

            // 드롭릿 세팅
            reactorCloudFoundryClient.applicationsV3().setCurrentDroplet(SetApplicationCurrentDropletRequest.builder().applicationId(applicationid).data(Relationship.builder().id(reactorCloudFoundryClient.builds().get(GetBuildRequest.builder().buildId(buildResponse.getId()).build()).block().getDroplet().getId()).build()).build()).block();

        } catch (Exception e) {
            LOGGER.error(e.toString());
            throw new Exception("App Build Time Over");
        }
        return new HashMap<String, Object>() {{
            put("RESULT", Constants.RESULT_STATUS_SUCCESS);
        }};
    }

    /**
     * 앱 인스턴스를 변경한다.
     *
     * @param app   the app
     * @param token the client
     * @the exception
     */
    public Map updateApp(App app, String token) {
        Map resultMap = new HashMap();
        try {
            ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(tokenProvider(token));
            if (app.getInstances() > 0) {
                cloudFoundryClient.applicationsV2().update(org.cloudfoundry.client.v2.applications.UpdateApplicationRequest.builder().applicationId(app.getGuid().toString()).instances(app.getInstances()).build()).block();
            }
            if (app.getMemory() > 0) {
                cloudFoundryClient.applicationsV2().update(org.cloudfoundry.client.v2.applications.UpdateApplicationRequest.builder().applicationId(app.getGuid().toString()).memory(app.getMemory()).build()).block();
            }
            if (app.getDiskQuota() > 0) {
                cloudFoundryClient.applicationsV2().update(org.cloudfoundry.client.v2.applications.UpdateApplicationRequest.builder().applicationId(app.getGuid().toString()).diskQuota(app.getDiskQuota()).build()).block();
            }
            if (app.getName() != null && !app.getName().equals("")) {
                cloudFoundryClient.applicationsV2().update(org.cloudfoundry.client.v2.applications.UpdateApplicationRequest.builder().applicationId(app.getGuid().toString()).name(app.getName()).build()).block();
            }
            if (app.getEnvironment() != null && app.getEnvironment().size() > 0) {
                cloudFoundryClient.applicationsV2().update(org.cloudfoundry.client.v2.applications.UpdateApplicationRequest.builder().applicationId(app.getGuid().toString()).environmentJsons(app.getEnvironment()).build()).block();
            } else if (app.getEnvironment() != null && app.getEnvironment().size() == 0) {
                cloudFoundryClient.applicationsV2().update(UpdateApplicationRequest.builder().applicationId(app.getGuid().toString()).environmentJsons(new HashMap<>()).build()).block();
            }

            resultMap.put("result", true);
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", false);
            resultMap.put("msg", e);
        }

        return resultMap;
    }

    /**
     * 앱-서비스를 바인드한다.
     *
     * @param body
     * @param token the client
     * @the exception
     */
    public Map bindService(Map body, String token) {
        Map resultMap = new HashMap();

        try {
            ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(tokenProvider(token));

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> parameterMap = mapper.readValue(body.get("parameter").toString(), new TypeReference<Map<String, Object>>() {
            });

            cloudFoundryClient.serviceBindingsV2().create(CreateServiceBindingRequest.builder().applicationId(body.get("applicationId").toString()).serviceInstanceId(body.get("serviceInstanceId").toString()).parameters(parameterMap).build()).block();

            resultMap.put("result", true);

        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", false);
            resultMap.put("msg", e);
        }

        return resultMap;
    }


    /**
     * 앱-서비스를 언바인드한다.
     *
     * @param serviceInstanceId
     * @param applicationId
     * @param token             the client
     * @the exception
     */
    public Map unbindService(String serviceInstanceId, String applicationId, String token) {
        Map resultMap = new HashMap();

        try {
            ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(tokenProvider(token));

            ListServiceInstanceServiceBindingsResponse listServiceInstanceServiceBindingsResponse = cloudFoundryClient.serviceInstances().listServiceBindings(ListServiceInstanceServiceBindingsRequest.builder().applicationId(applicationId).serviceInstanceId(serviceInstanceId).build()).block();
            String instancesServiceBindingGuid = listServiceInstanceServiceBindingsResponse.getResources().get(0).getMetadata().getId();

            DeleteServiceBindingResponse deleteServiceBindingResponse = cloudFoundryClient.serviceBindingsV2().delete(DeleteServiceBindingRequest.builder().serviceBindingId(instancesServiceBindingGuid).build()).block();
            LOGGER.info("deleteServiceBindingResponse :", deleteServiceBindingResponse);

            resultMap.put("result", true);

        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", false);
            resultMap.put("msg", e);
        }

        return resultMap;
    }

    /**
     * 앱-유저 프로바이드 서비스를 언바인드한다.
     *
     * @param serviceInstanceId
     * @param applicationId
     * @param token             the client
     * @the exception
     */
    public Map unbindUserProvideService(String serviceInstanceId, String applicationId, String token) {
        Map resultMap = new HashMap();

        try {
            ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(tokenProvider(token));
            ListUserProvidedServiceInstanceServiceBindingsResponse listUserProvidedServiceInstanceServiceBindingsResponse = cloudFoundryClient.userProvidedServiceInstances().listServiceBindings(ListUserProvidedServiceInstanceServiceBindingsRequest.builder().applicationId(applicationId).userProvidedServiceInstanceId(serviceInstanceId).build()).block();
            String instancesUserProvidedServiceBindingGuid = listUserProvidedServiceInstanceServiceBindingsResponse.getResources().get(0).getMetadata().getId();
            DeleteServiceBindingResponse deleteServiceBindingResponse = cloudFoundryClient.serviceBindingsV2().delete(DeleteServiceBindingRequest.builder().serviceBindingId(instancesUserProvidedServiceBindingGuid).build()).block();
            LOGGER.info("deleteServiceBindingResponse :", deleteServiceBindingResponse);
            resultMap.put("result", true);

        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", false);
            resultMap.put("msg", e);
        }

        return resultMap;
    }


    /**
     * 앱 이벤트를 조회한다.
     *
     * @param guid
     * @param token the client
     * @return the app events
     * @the exception
     */
    public ListEventsResponse getAppEvents(String guid, String token) {
        ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(tokenProvider(token));

        ListEventsRequest.Builder requestBuilder = ListEventsRequest.builder().actee(guid).resultsPerPage(100).orderDirection(OrderDirection.DESCENDING);
        ListEventsResponse listEventsResponse = cloudFoundryClient.events().list(requestBuilder.build()).block();

        return listEventsResponse;
    }

    /**
     * 앱 환경변수를 조회한다.
     *
     * @param guid
     * @param token the token
     * @return the application env
     * @the exception
     * @version 1.0
     * @since 2016.6.29 최초작성
     */
    public ApplicationEnvironmentResponse getApplicationEnv(String guid, String token) {
        ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(tokenProvider(token));

        ApplicationEnvironmentResponse applicationEnvironmentResponse = cloudFoundryClient.applicationsV2().environment(ApplicationEnvironmentRequest.builder().applicationId(guid).build()).block();

        return applicationEnvironmentResponse;
    }

    /**
     * 라우트 추가 및 라우트와 앱을 연결한다. (앱에 URI를 추가함)
     *
     * @param body
     * @param token the token
     * @return the boolean
     * @the exception
     * @version 1.0
     * @since 2016.7.6 최초작성
     */
    public Map addApplicationRoute(Map body, String token) {
        Map resultMap = new HashMap();

        try {
            ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(tokenProvider(token));

            CreateRouteResponse createRouteResponse = cloudFoundryClient.routes().create(CreateRouteRequest.builder().host(body.get("host").toString()).domainId(body.get("domainId").toString()).spaceId(body.get("spaceId").toString()).build()).block();

            cloudFoundryClient.routeMappings().create(CreateRouteMappingRequest.builder().applicationId(body.get("applicationId").toString()).routeId(createRouteResponse.getMetadata().getId()).build()).block();

            resultMap.put("result", true);
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", false);
            resultMap.put("msg", e);
        }

        return resultMap;
    }

    /**
     * 앱 라우트를 해제한다.
     *
     * @param guid
     * @param route_guid
     * @param token      the token
     * @return the boolean
     * @the exception
     * @version 1.0
     * @since 2016.7.6 최초작성
     */
    public Map removeApplicationRoute(String guid, String route_guid, String token) {
        Map resultMap = new HashMap();

        try {
            ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(tokenProvider(token));

            cloudFoundryClient.applicationsV2().removeRoute(RemoveApplicationRouteRequest.builder().applicationId(guid).routeId(route_guid).build()).block();

            cloudFoundryClient.routes().delete(DeleteRouteRequest.builder().routeId(route_guid).build()).block();

            resultMap.put("result", true);
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", false);
            resultMap.put("msg", e);
        }

        return resultMap;
    }

    /**
     * 인덱스로 앱 인스턴스를 종료한다.
     *
     * @param guid
     * @param index
     * @param token
     * @return the map
     * @the exception
     */
    public Map terminateInstance(String guid, String index, String token) {
        Map resultMap = new HashMap();

        try {
            ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(tokenProvider(token));

            org.cloudfoundry.client.v2.applications.TerminateApplicationInstanceRequest.Builder requestBuilder = TerminateApplicationInstanceRequest.builder();
            requestBuilder.applicationId(guid);
            requestBuilder.index(index);
            cloudFoundryClient.applicationsV2().terminateInstance(requestBuilder.build()).block();

            resultMap.put("result", true);
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", false);
            resultMap.put("msg", e);
        }

        return resultMap;
    }

    /**
     * App 로그를 확인한다. (REST API 사용)
     *
     * @param guid
     * @param time
     * @param limit
     * @param isDescending
     * @param envelope_types
     * @param token
     * @return Batch
     * @the exception
     */
    public Batch getLog(String guid, String time, int limit, boolean isDescending, String envelope_types, String token) {
        TokenProvider tokenProvider = tokenProvider(token);
        
        String reqUrl = logCacheTarget + "/api/v1/read/" + guid  + "?" + (isDescending ? "descending=true&" : "") + "envelope_types=" + envelope_types + (limit == 0  ? "" : "&limit=" + limit) +"&start_time=" + time;
        Map logmap = restTemplateService.cfSend(token, reqUrl, HttpMethod.GET, null, Map.class);

        ObjectMapper mapper = new ObjectMapper();
        Batch batch = mapper.convertValue(logmap.get("envelopes"), Batch.class);
        return batch;
    }


    public Map userProvideCredentials(String guid, String token) {
        Map resultMap = new HashMap();
        ArrayList resultlist = new ArrayList();
        ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(tokenProvider(token));

        GetUserProvidedServiceInstanceResponse getUserProvidedServiceInstanceResponse = cloudFoundryClient.userProvidedServiceInstances().get(GetUserProvidedServiceInstanceRequest.builder().userProvidedServiceInstanceId(guid).build()).block();
        String str = getUserProvidedServiceInstanceResponse.getEntity().getCredentials().toString();
        str = str.replace("{", "");
        str = str.replace("}", "");
        str = str.replace(" ", "");
        String[] str2 = str.split(",");
        for (String strs : str2) {
            Map listStr = new HashMap();
            String[] str3 = strs.split("=");
            listStr.put("key", str3[0]);
            listStr.put("value", str3[1]);
            resultlist.add(listStr);
        }
        resultMap.put("List", resultlist);
        return resultMap;
    }

    /**
     * 앱을 실행한다.
     *
     * @param token the client
     * @throws Exception the exception
     *                   권한:사용자 권한
     */
    public Map startApp(String appGuid, String token) {
        Map resultMap = new HashMap();

        try {
            ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(tokenProvider(token));
            cloudFoundryClient.applicationsV3().start(StartApplicationRequest.builder().applicationId(appGuid).build()).block();
            resultMap.put("result", true);
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", false);
            resultMap.put("msg", e.getMessage());
        }
        return resultMap;

    }

    /**
     * 앱을 중지한다.
     *
     * @param token the client
     * @return ModelAndView model
     * @throws Exception the exception
     *                   권한:사용자 권한
     */
    public Map stopApp(String appGuid, String token) {
        Map resultMap = new HashMap();

        try {
            ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(tokenProvider(token));
            cloudFoundryClient.applicationsV3().stop(StopApplicationRequest.builder().applicationId(appGuid).build()).block();
            resultMap.put("result", true);
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", false);
            resultMap.put("msg", e.getMessage());
        }

        return resultMap;

    }

    /**
     * App env 변수 업데이트
     *
     * @param app   the app
     * @param token the token
     * @return UpdateApplicationEnvironmentVariablesResponse
     * <p>
     * 권한 : 사용자 권한
     */
    public UpdateApplicationEnvironmentVariablesResponse setAppEnv(App app, String token) {
        //LOGGER.info("변경사항 있는 환경변수들 ::: " + app.getEnvironment().toString());

        ReactorCloudFoundryClient reactorCloudFoundryClient = cloudFoundryClient(tokenProvider(token));
        UpdateApplicationEnvironmentVariablesResponse updatedAppEnvVar = reactorCloudFoundryClient.applicationsV3().updateEnvironmentVariables(UpdateApplicationEnvironmentVariablesRequest.builder().applicationId(app.getGuid().toString()).putAllVars(app.getEnvironment()).build()).block();

        //LOGGER.info("변경사항 있는 환경변수들은요~~~ ::: " + updatedAppEnvVar.toString());
        return updatedAppEnvVar;
    }

    /**
     * 앱 실시간 상태를 조회한다.
     *
     * @param guid  the app guid
     * @param cloudFoundryClient the ReactorCloudFoundryClient
     * @return the app stats
     */
    public ApplicationStatisticsResponse getAppStats(String guid, ReactorCloudFoundryClient cloudFoundryClient) {

        ApplicationStatisticsResponse applicationStatisticsResponse = cloudFoundryClient.applicationsV2().statistics(ApplicationStatisticsRequest.builder().applicationId(guid).build()).block();

        return applicationStatisticsResponse;
    }

    /**
     * 앱 실시간 상태를 조회한다.
     *
     * @param guid  the app guid
     * @param cloudFoundryClient the ReactorCloudFoundryClient
     * @return the app stats
     */
    public GetApplicationProcessStatisticsResponse getAppStatsV3(String guid, ReactorCloudFoundryClient cloudFoundryClient) {

        GetApplicationProcessStatisticsResponse applicationStatisticsResponse =
                cloudFoundryClient.applicationsV3().getProcessStatistics(GetApplicationProcessStatisticsRequest.builder().applicationId(guid).type("web").build()).block();

        return applicationStatisticsResponse;
    }

    /**
     * 앱 프로세스 리스트를 조회한다.
     *
     * @param guid  the app guid
     * @param cloudFoundryClient the ReactorCloudFoundryClient
     * @return the app stats
     */
    public ListApplicationProcessesResponse getListApplicationProcess(String guid, ReactorCloudFoundryClient cloudFoundryClient) {
        ListApplicationProcessesResponse applicationProcessesResponse =
                cloudFoundryClient.applicationsV3().listProcesses(ListApplicationProcessesRequest.builder().applicationId(guid).build()).block();
        return applicationProcessesResponse;
    }

    /**
     * 앱 서비스 리스트를 조회한다
     *
     * @param guid  the app guid
     * @param cloudFoundryClient the ReactorCloudFoundryClient
     * @return List<ServiceV3>
     */
    public List<ServiceV3> getServiceList(String appGuid, String token) {
        ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(tokenProvider(token));
        ListServiceBindingsResponse listServiceBindingsResponse = cloudFoundryClient.serviceBindingsV3().list(ListServiceBindingsRequest.builder().applicationId(appGuid).build()).block();
        String service_guid = null;
        String service_plan_guid = null;
        String service_offering_id = null;
        String service_offering_name = null;
        ServiceV3 service = null;
        List<ServiceV3> serviceArray = new ArrayList<>();


        for (int i=0; i<listServiceBindingsResponse.getResources().size(); i++){

            service = new ServiceV3();
            //service guid
            service_guid = listServiceBindingsResponse.getResources().get(i).getRelationships().getServiceInstance().getData().getId();
            //service_plan_guid
            service_plan_guid = cloudFoundryClient.serviceInstancesV3().get(GetServiceInstanceRequest.builder().serviceInstanceId(service_guid).build()).block().getRelationships().getServicePlan().getData().getId();
            //service_offering_id
            service_offering_id = cloudFoundryClient.servicePlansV3().get(GetServicePlanRequest.builder().servicePlanId(service_plan_guid).build()).block().getRelationships().getServiceOffering().getData().getId();
            //service_offering_name
            service_offering_name = cloudFoundryClient.serviceOfferingsV3().get(GetServiceOfferingRequest.builder().serviceOfferingId(service_offering_id).build()).block().getName();

            service.setName(cloudFoundryClient.serviceInstancesV3().get(GetServiceInstanceRequest.builder().serviceInstanceId(service_guid).build()).block().getName());
            service.setGuid(UUID.fromString(service_guid));
            GetServicePlanResponse getServicePlanResponse = cloudFoundryClient.servicePlansV3().get(GetServicePlanRequest.builder().servicePlanId(service_plan_guid).build()).block();
            service.setService_plan(new ServiceV3.ServicePlan(getServicePlanResponse.getName(), new ServiceV3.ServicePlan.ServiceInfo(service_offering_name)));
            serviceArray.add(service);
        }

        return serviceArray;
    }
}
