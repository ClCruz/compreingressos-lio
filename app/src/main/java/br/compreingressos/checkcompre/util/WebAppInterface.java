package br.compreingressos.checkcompre.util;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;

import java.sql.SQLException;

import br.compreingressos.checkcompre.dao.OrderDao;
import br.compreingressos.checkcompre.helper.DatabaseHelper;
import br.compreingressos.checkcompre.helper.OrderHelper;
import br.compreingressos.checkcompre.interfaces.WebAppInterfaceListener;

/**
 * Created by luiszacheu on 17/03/15.
 */
public class WebAppInterface {
    public static final String LOG_TAG = "WebAppInterface";
    Context context;
    private OrderDao orderDao;
    private DatabaseHelper databaseHelper;
    public static WebAppInterfaceListener webAppInterfaceListener;

    public WebAppInterface(Context c){
        context = c;
    }

    @JavascriptInterface
    public void getInfoPagamento(String resultJson){
//        databaseHelper =  new DatabaseHelper(context);
//
//        try {
//            orderDao = new OrderDao(databaseHelper.getConnectionSource());
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//        try {
//            Log.i(LOG_TAG, resultJson);
//            orderDao.create(OrderHelper.loadOrderFromJSON(resultJson));
//            print(resultJson);
//        } catch (SQLException e) {
//            Log.e(LOG_TAG, "nao foi possivel salvar o pedido");
//            e.printStackTrace();
//        }
        print(resultJson);
    }

    public void loadDataResultFromWebviewListener(WebAppInterfaceListener listener){
        webAppInterfaceListener=listener;
    }

    public void print(String result){
        System.out.println(webAppInterfaceListener.onFinishLoadResultData(result));
    }

    @JavascriptInterface
    public void getItemsGoogleAnalytics(String result){
        webAppInterfaceListener.onLoadItemsGoogleAnalytics(result);

    }


}
