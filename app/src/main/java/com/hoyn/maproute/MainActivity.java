package com.hoyn.maproute;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.MyLocationStyle;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkRouteResult;

import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationSource,
        AMapLocationListener, AMap.OnMapClickListener, RouteSearch.OnRouteSearchListener {
    private AMap aMap;
    private MapView mapView;
    private MarkerOptions markerOption;
    private LocationSource.OnLocationChangedListener mListener;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;
    private RouteSearch mRouteSearch;

    private Context context;
    boolean canAddMaker = false;
    private boolean isFirstPoint = false;

    private LatLng locPosition;
    private String firstTitle = "";
    private LatLng locFirst;
    private String secondTitle = "";
    private LatLng locSecond;
    private GeocodeSearch geocoderSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        context = this;

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 此方法必须重写
        init();

        findViewById(R.id.btn_select_first).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFirstPoint = true;
                aMap.clear();
                //再次点击的
                showToast("现在可以在地图上选取第一点了");
                canAddMaker = true;
                reShowMaker();
            }
        });

        //选取第二点
        findViewById(R.id.btn_select_second).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFirstPoint = false;
                //再次点击的
                aMap.clear();
                showToast("现在可以在地图上选取第二点了");
                canAddMaker = true;
                reShowMaker();
            }
        });
        //计算距离
        findViewById(R.id.btn_getDistance).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgressDialog();
                searchRouteStart();
            }
        });
    }

    /**
     * 初始化AMap对象
     */
    private void init() {
        geocoderSearch = new GeocodeSearch(this);
        mRouteSearch = new RouteSearch(this);
        mRouteSearch.setRouteSearchListener(this);
        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.moveCamera(CameraUpdateFactory.zoomTo(20));
            aMap.setOnMapClickListener(this);
            setUpMap();
        }
    }

    /**
     * 设置一些amap的属性
     */
    private void setUpMap() {
        // 自定义系统定位小蓝点
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory
                .fromResource(R.drawable.poi_marker_pressed));// 设置小蓝点的图标
//        myLocationStyle.strokeColor(Color.BLACK);// 设置圆形的边框颜色
//        myLocationStyle.radiusFillColor(Color.argb(100, 0, 0, 180));// 设置圆形的填充颜色
        // myLocationStyle.anchor(int,int)//设置小蓝点的锚点
//        myLocationStyle.strokeWidth(1.0f);// 设置圆形的边框粗细
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setLocationSource(this);// 设置定位监听
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        // aMap.setMyLocationType()
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        deactivate();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    /**
     * 定位成功后回调函数
     */
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null) {
            if (amapLocation != null
                    && amapLocation.getErrorCode() == 0) {
                mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
                locPosition = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
                mlocationClient.stopLocation();
            } else {
                String errText = "定位失败," + amapLocation.getErrorCode() + ": " + amapLocation.getErrorInfo();
                Log.e("AmapErr", errText);
            }
        }
    }

    /**
     * 激活定位
     */
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            mlocationClient = new AMapLocationClient(this);
            mLocationOption = new AMapLocationClientOption();
            //设置定位监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();
        }
    }

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }


    private void showToast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }


    /**
     * 在地图上添加marker
     */
    private void addMarkersToMap(final LatLng latlng) {
        LatLonPoint latLonPoint = new LatLonPoint(latlng.latitude, latlng.longitude);
        RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 200, GeocodeSearch.AMAP);
        geocoderSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
            /**
             * 逆地理编码回调
             */
            @Override
            public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
                if (rCode == 1000) {
                    if (result != null && result.getRegeocodeAddress() != null
                            && result.getRegeocodeAddress().getPois().size() > 0) {
                        List<PoiItem> poiItems = result.getRegeocodeAddress().getPois();
                        String title = poiItems.get(0).getTitle();

                        if (isFirstPoint) {
                            locFirst = latlng;
                            firstTitle = title;
                        } else {
                            locSecond = latlng;
                            secondTitle = title;
                        }
                        //文字显示标注，可以设置显示内容，位置，字体大小颜色，背景色旋转角度,Z值等
                        markerOption = new MarkerOptions();
                        markerOption.position(latlng);
                        markerOption.title(title);
                        markerOption.draggable(true);
                        markerOption.icon(BitmapDescriptorFactory
                                .fromResource(R.drawable.poi_marker_pressed));
                        aMap.addMarker(markerOption).showInfoWindow();
//                        marker2 = aMap.addMarker(markerOption);
//                        marker2.showInfoWindow();
                        reShowMaker();

                    } else {
                        showToast("没有搜索到周边建筑");
                    }
                } else {
                    showToast("没有搜索到周边建筑");
                }

            }

            @Override
            public void onGeocodeSearched(GeocodeResult arg0, int arg1) {

            }

        });
        geocoderSearch.getFromLocationAsyn(query);
    }

    /**
     * 在地图上添加marker
     */
    private void addMarkersToMap(final LatLng latlng, String title) {
        //文字显示标注，可以设置显示内容，位置，字体大小颜色，背景色旋转角度,Z值等
        markerOption = new MarkerOptions();
        markerOption.position(latlng);
        markerOption.title(title);
        markerOption.draggable(true);
        markerOption.icon(BitmapDescriptorFactory
                .fromResource(R.drawable.poi_marker_pressed));
        aMap.addMarker(markerOption).showInfoWindow();
//        marker2 = aMap.addMarker(markerOption);
//        marker2.showInfoWindow();
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (!canAddMaker) {
            return;
        }
        //新增坐标覆盖物
        addMarkersToMap(latLng);
        canAddMaker = false;

    }

    //清空之后重新显示
    private void reShowMaker() {
        //清空之后重新显示
        aMap.clear();
        //后添加的会显示title而先添加的不会
        if (isFirstPoint) {
            if (locSecond != null) {
                addMarkersToMap(locSecond, secondTitle);
            }
            if (locFirst != null) {
                addMarkersToMap(locFirst, firstTitle);
            }
        } else {
            if (locFirst != null) {
                addMarkersToMap(locFirst, firstTitle);
            }
            if (locSecond != null) {
                addMarkersToMap(locSecond, secondTitle);
            }
        }
    }

    private ProgressDialog progDialog = null;// 搜索时进度条

    /**
     * 显示进度框
     */
    private void showProgressDialog() {
        if (progDialog == null)
            progDialog = new ProgressDialog(this);
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setIndeterminate(false);
        progDialog.setCancelable(true);
        progDialog.setMessage("正在搜索");
        progDialog.show();
    }

    /**
     * 隐藏进度框
     */
    private void dissmissProgressDialog() {
        if (progDialog != null) {
            progDialog.dismiss();
        }
    }

    /**
     * 开始搜索路径规划方案
     */
    public void searchRouteStart() {
        if (locFirst == null) {
            showToast("没有设置起点");
            return;
        }
        if (secondTitle == null) {
            showToast("没有设置终点");
            return;
        }
        showProgressDialog();
        final RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(
                new LatLonPoint(locFirst.latitude, locFirst.longitude), new LatLonPoint(locSecond.latitude, locSecond.longitude));
        RouteSearch.DriveRouteQuery query = new RouteSearch.DriveRouteQuery(fromAndTo, RouteSearch.DrivingDefault, null,
                null, "");// 第一个参数表示路径规划的起点和终点，第二个参数表示驾车模式，第三个参数表示途经点，第四个参数表示避让区域，第五个参数表示避让道路
        mRouteSearch.calculateDriveRouteAsyn(query);// 异步路径规划驾车模式查询
    }

    @Override
    public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

    }

    @Override
    public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int errorCode) {
        dissmissProgressDialog();
        if (errorCode == 1000) {
            if (driveRouteResult != null && driveRouteResult.getPaths() != null) {
                if (driveRouteResult.getPaths().size() > 0) {
                    final DrivePath drivePath = driveRouteResult.getPaths()
                            .get(0);
                    int dis = (int) drivePath.getDistance();
                    showToast("距离 : "+dis + "米");
                }
            }
        }
    }

    @Override
    public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {

    }
}
