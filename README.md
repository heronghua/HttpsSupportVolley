# HttpsSupportVolley

支持Https TLS1.0的Volley 在原來代碼的基礎上，添加類 SSLSocketFactoryExtended.java ,

## Volley.java 添加了支持不受信任證書的功能，使用方法：
RequestQueue queue = Volley.newRequestQueue(context.getApplicationContext(),null, true,R.raw.xxx);
請將證書導入放到raw 目錄下


## 添加了VolleyImageCache 和DiskLruCache 類 實現了圖片三級緩存，使用方法：

ImageLoader loader = new ImageLoader(requestQueue, new VolleyImageCache(context));
networkImageView.setImageUrl(url, loader);

