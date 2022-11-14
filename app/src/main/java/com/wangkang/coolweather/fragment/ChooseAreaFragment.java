package com.wangkang.coolweather.fragment;

import android.app.ProgressDialog;
import android.net.wifi.aware.PublishConfig;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.wangkang.coolweather.R;
import com.wangkang.coolweather.db.City;
import com.wangkang.coolweather.db.County;
import com.wangkang.coolweather.db.Province;
import com.wangkang.coolweather.util.HttpUtil;
import com.wangkang.coolweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {

    public static final int LEAVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;

    private TextView titleText;

    private Button backButton;

    private ListView listView;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();

    private List<Province> provincesList = new ArrayList<>();

    private List<City> cityList = new ArrayList<>();

    private List<County> countyList = new ArrayList<>();

    private Province selecteProvince;

    private City selecteCity;

    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_buton);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(((parent, view, position, id) -> {
            if (currentLevel == LEAVEL_PROVINCE) {
                selecteProvince = provincesList.get(position);
                queryCities();
            } else if (currentLevel == LEVEL_CITY) {
                selecteCity = cityList.get(position);
                queryCounties();
            }
        }));
        backButton.setOnClickListener(v -> {
            if (currentLevel == LEVEL_COUNTY) {
                queryCities();
            } else if(currentLevel == LEVEL_CITY) {
                queryProvinces();
            }
        });
        queryProvinces();
    }

    /**
     * 查询全国所有的省，优先从数据库查，如果没有再去服务器查
     */
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provincesList = DataSupport.findAll(Province.class);
        if (provincesList.size() > 0) {
            dataList.clear();
            for (Province province : provincesList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEAVEL_PROVINCE;
        } else {
            //从服务器查询省级数据
            String address = "http://guolin.tech/api/china";
            queryFromServer(address,LEAVEL_PROVINCE);
        }
    }

    private void queryCities(){
        titleText.setText(selecteProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?", String.valueOf(selecteProvince.getId()))
                .find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            //从服务器查询市级数据
            int provinceCode = selecteProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address,LEVEL_CITY);
        }
    }

    private void queryCounties(){
        titleText.setText(selecteCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?", String.valueOf(selecteCity.getId()))
                .find(County.class);
        if (countyList.size() > 0){
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selecteProvince.getProvinceCode();
            int cityCode = selecteCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, LEVEL_COUNTY);
        }
    }

    private void queryFromServer(String address, final int _currentLevel){
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                //根据当前等级分别处理服务器返回的json并保存到数据库
                switch (_currentLevel){
                    case LEAVEL_PROVINCE:
                        result = Utility.handleProvinceResponse(responseText);
                        break;
                    case LEVEL_CITY:
                        result = Utility.handleCityResponse(responseText, selecteProvince.getId());
                        break;
                    case LEVEL_COUNTY:
                        result = Utility.handleCountyResponse(responseText,selecteCity.getId());
                        break;
                    default:
                }
                //重新到数据库查询数据
                if (result) {
                    getActivity().runOnUiThread(() -> {
                        //empty
                        switch (_currentLevel){
                            case LEAVEL_PROVINCE:
                                queryProvinces();
                                break;
                            case LEVEL_CITY:
                                queryCities();
                                break;
                            case LEVEL_COUNTY:
                                queryCounties();
                                break;
                            default:
                        }
                    });
                    closeProgressDialog();
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                getActivity().runOnUiThread(() -> {
                    closeProgressDialog();
                    Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT)
                            .show();
                });
            }
        });
    }

    private void showProgressDialog(){
        if (progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog(){
        if (progressDialog != null){
            progressDialog.dismiss();
        }
    }
}