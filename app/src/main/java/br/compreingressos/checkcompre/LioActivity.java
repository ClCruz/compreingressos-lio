package br.compreingressos.checkcompre;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import br.compreingressos.checkcompre.interfaces.WebAppInterfaceListener;
import br.compreingressos.checkcompre.util.AndroidUtils;
import br.compreingressos.checkcompre.util.WebAppInterface;
import cielo.orders.domain.Credentials;
import cielo.orders.domain.Environment;
import cielo.orders.domain.Order;
import cielo.sdk.order.OrderManager;
import cielo.sdk.order.payment.PaymentError;
import cielo.sdk.order.payment.PaymentListener;

public class LioActivity extends Activity {

    private static final String URL = "http://homolog.compreingressos.com:8081/mobile/lio.php?";
    private static final String TAG = "LIO_ACTIVITY";

    private OrderManager orderManager;
    private Order order;

    private WebView webView;
    private Button buttonAvancar;
    private ProgressBar progressBar;
    private AlertDialog alertDialog;
    public WebAppInterface webAppInterface;

    private String idUser, local, evento, evento_nome, apresentacao, horario, parameters;
    private boolean pagamento;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lio);

        conifgSDK();
        initialize();

        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setBuiltInZoomControls(true);

        WebSettings settings = webView.getSettings();
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        webAppInterface = new WebAppInterface(this);

        webView.addJavascriptInterface(webAppInterface, "Android");

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {

                try {
                   webView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }

                hideNextButton();

                if (url.contains("etapa1.php")) {
                    showNextButton();
                } else if (url.contains("etapa2.php")) {
                    showNextButton();
                } else if (url.contains("etapa3.php")) {
                    hideNextButton();
                } else if (url.contains("etapa4.php")) {
                    showNextButton();
                } else if (url.contains("etapa5.php")) {
                    showNextButton();
                    buttonAvancar.setText("Pagar");
                } else if (url.contains("pagamento_ok.php")) {
                    buttonAvancar.setText("Finalizar");
                    showNextButton();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, String url) {
                final String URLNext = url;

                if (url.contains("etapa1.php")) {
                    showNextButton();
                    buttonAvancar.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                        StringBuilder scriptOnClick = new StringBuilder("javascript:var length = $('.container_botoes_etapas').find('a').length;");
                        scriptOnClick.append("$('.container_botoes_etapas').find('a')[length - 2].click(); ");
                        view.loadUrl(scriptOnClick.toString());
                        }
                    });
                } else if (url.contains("etapa5.php")) {
                    showNextButton();
                    buttonAvancar.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                        makePayment();
                        }
                    });

                    if(pagamento){ view.loadUrl(url); }
                } else if (url.contains("pagamento_ok.php")) {
                    showNextButton();
                    buttonAvancar.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            finish();
                        }
                    });
                }

                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {

                view.loadUrl("javascript:$(\"script[src*='visitor.js']\").remove();");

                try {
                    if (progressBar.isShown()) {
                        webView.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }

                webView.loadUrl("javascript:document.getElementById('novo_menu').style.display='none';void(0);");
                webView.loadUrl("javascript:document.getElementById('content').style.overflowX='hidden';void(0);");

                if (url.contains("/comprar/etapa3_2.php")) {
                    webView.loadUrl("javascript:document.getElementById('checkbox_sms').style.display='none';void(0);");
                    webView.loadUrl("javascript:document.getElementById('checkbox_politica').style.display='none';void(0);");
                }

                if (url.contains("/comprar/assinatura.php")) {
                    webView.loadUrl("javascript:document.getElementById('botoes').style.paddingBottom='50px';void(0);");
                } else {
                    webView.loadUrl("javascript:document.getElementById('content').style.paddingBottom='300px';void(0);");
                }

                webView.loadUrl("javascript:document.getElementById('footer').style.display='none';void(0);");

                if (url.contains("/comprar/assinatura_ok.php")) {
                    webView.loadUrl("javascript:document.getElementsByClassName('nova_venda').item(0).style.display='none';void(0);");
                    webView.loadUrl("javascript:for(i = 0; i < document.getElementsByTagName('a').length; i++){ document.getElementsByTagName('a').item(i).style.color='black';}void(0);");
                }

                if(url.contains("etapa4.php")) {
                    webView.loadUrl(runScript());
                    webAppInterface.loadDataResultFromWebviewListener(new WebAppInterfaceListener() {
                        @Override
                        public String onFinishLoadResultData(String resultData) {
                            Log.i(TAG, resultData);
                            trackOrderWithItemsOnGA(resultData);
                            return resultData;
                        }

                        @Override
                        public String onLoadItemsGoogleAnalytics(String resultData) {
                            trackOrderWithItemsOnGA(resultData);
                            return resultData;
                        }
                    });
                }

            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e(TAG, "Error: " + description);
                //Toast.makeText(, "Oh no! " + description, Toast.LENGTH_SHORT).show();
                alertDialog.setTitle("Error");
                alertDialog.setMessage(description);
                alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });
                alertDialog.show();
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                view.loadUrl("javascript:$(\"#menu_topo\").hide();$('.aba' && '.fechado').hide();$(\"#footer\").hide();$(\"#selos\").hide();");
                view.loadUrl("javascript:$('.imprima_agora').hide();");
                view.loadUrl("javascript:$('.presente').hide();");
                view.loadUrl("javascript:$('.parcelas').hide();");

                view.loadUrl("javascript:$(document).ready(function(){$('.voltar').hide();});");

                if (AndroidUtils.isKitKatOrNewer()) {
                    view.loadUrl("javascript:$(document).ready(function(){$('.container_botoes_etapas').hide();});");
                } else {
                    view.loadUrl("javascript:$(document).ready(function(){$('.container_botoes_etapas').show();});");
                }
            }

        });
        webView.loadUrl(URL + parameters);
    }

    public void conifgSDK() {
        Map<String, Object> options = new HashMap<>();
        Credentials credentials = new Credentials("CApPVmVpYibJ", "BjpzG7P9ieu8");
        orderManager = new OrderManager(credentials, this);
        orderManager.bind(this, null);
    }

    public void initialize(){
        Bundle p = getIntent().getExtras();
        idUser = p.getString("idUser");
        local = p.getString("local");
        evento = p.getString("evento");
        evento_nome = p.getString("evento_nome");
        apresentacao = p.getString("apresentacao");
        horario = p.getString("horario").replace(":", "h");

        buttonAvancar = (Button) findViewById(R.id.btn_avancar);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        webView = (WebView) findViewById(R.id.webView);

        alertDialog = new AlertDialog.Builder(this).create();

        getActionBar().setTitle(evento_nome);

        parameters = "action=venda&local=" + local + "&evento=" + evento + "&apresentacao=" + apresentacao + "&horario=" + horario + "&id_usuario=" + idUser;
    }

    public void makePayment() {

        if (order != null) {

            orderManager.placeOrder(order);
            orderManager.checkoutOrder(order.getId(), order.getPrice(), new PaymentListener() {

                @Override
                public void onStart() {
                    Log.d(TAG, "ON START");
                }

                @Override
                public void onPayment(@NonNull Order paidOrder) {
                    Log.d(TAG, "ON PAYMENT");

                    order = paidOrder;
                    order.markAsPaid();
                    orderManager.updateOrder(order);
                    pagamento = true;

                    StringBuilder scriptOnClick = new StringBuilder("javascript:var length = $('.container_botoes_etapas').find('a').length;");
                    scriptOnClick.append("$('.container_botoes_etapas').find('a')[length - 2].click(); ");
                    webView.loadUrl(scriptOnClick.toString());
                    resetState();
                    hideNextButton();
                }

                @Override
                public void onCancel() {
                    Log.d(TAG, "ON CANCEL");
                }

                @Override
                public void onError(@NonNull PaymentError paymentError) {
                    Log.d(TAG, "ON ERROR");
                }

            });
        }
    }

    public String runScript() {
        StringBuilder scriptGetInfoPayment = new StringBuilder("javascript:var date_aux = new Array; \n");
        scriptGetInfoPayment.append("$('.data').children().each(function(){date_aux.push($(this).html())}); \n");
        scriptGetInfoPayment.append("var order_date = date_aux.join(' '); \n");
        scriptGetInfoPayment.append("var spectacle_name = $('.resumo').find('.nome').html(); \n");
        scriptGetInfoPayment.append("var address = $('.resumo').find('.endereco').html(); \n");
        scriptGetInfoPayment.append("var theater = $('.resumo').find('.teatro').html(); \n");
        scriptGetInfoPayment.append("var time = $('.resumo').find('.horario').html(); \n");
        scriptGetInfoPayment.append("var order_total = $('.pedido_total').find('.valor').html(); \n");
        scriptGetInfoPayment.append("var payload = \n");
        scriptGetInfoPayment.append("{ \n");
        scriptGetInfoPayment.append("date:   order_date, \n");
        scriptGetInfoPayment.append("time:   time, \n");
        scriptGetInfoPayment.append("total:  order_total, \n");
        scriptGetInfoPayment.append("titulo:  spectacle_name \n");
        scriptGetInfoPayment.append("}; \n");
        scriptGetInfoPayment.append("Android.getInfoPagamento(JSON.stringify(payload));");
        return scriptGetInfoPayment.toString();
    }

    public void trackOrderWithItemsOnGA(String resultData) {
        String evento, data, time, total;

        if (!resultData.isEmpty()) {
            JSONObject json = null;
            try {
                json = new JSONObject(resultData);
                evento = json.getString("titulo");
                data = json.getString("date");
                time = json.getString("time");
                total = json.getString("total");
                order = orderManager.createDraftOrder(evento);
                if (order != null) {
                    order.addItem(evento, evento + " " + data + " " + time, Long.parseLong(total.replace(",", "")), 1, "EACH");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void resetState() {
        order = null;
    }

    private void showNextButton() {
        if (!buttonAvancar.isShown()) {
            buttonAvancar.setVisibility(View.VISIBLE);
        }
    }

    private void hideNextButton() {
        if (buttonAvancar.isShown()) {
            buttonAvancar.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_assinatura, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sair:
                finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
