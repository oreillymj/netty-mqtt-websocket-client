import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

public class SSLUtil {

    //Full cert chain of https://assets.emqx.com/data/broker.emqx.io-ca.crt  from here -  https://whatsmychaincert.com/?broker.emqx.io:8883
    final private String emqPEMchain = "-----BEGIN CERTIFICATE-----\n" +
            "MIIHiDCCBXCgAwIBAgIQAbRj6Y3xo6/OGk3Z8qbJJjANBgkqhkiG9w0BAQsFADBc\n" +
            "MQswCQYDVQQGEwJVUzEXMBUGA1UEChMORGlnaUNlcnQsIEluYy4xNDAyBgNVBAMT\n" +
            "K1JhcGlkU1NMIEdsb2JhbCBUTFMgUlNBNDA5NiBTSEEyNTYgMjAyMiBDQTEwHhcN\n" +
            "MjQwODA1MDAwMDAwWhcNMjUwODA1MjM1OTU5WjAUMRIwEAYDVQQDDAkqLmVtcXgu\n" +
            "aW8wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC7mBxIJ9piPb48aS12\n" +
            "ZOm3JPvaZy5qi7qBELUaHJEvYJ20fZbyIlYywvQ231KmdHXr37v84i/0G7eO//AX\n" +
            "TMbrYBddg9Vg/LNkyiQOTRiMRu+RpsuS1N/jU24YrcUkQRhUvH5PHg8O/NAI3f+h\n" +
            "wYogl5OzmNV1euPSIUDQAf/rm/UsyyXgIYzy8jSL5qqi7llY2dURCrCjKBDb7hW4\n" +
            "IdCan0HGBD6GcWV1ORI+ROsTFjqD205CGlnqZdms8lnnA6a08IUkfcYYcjjAq0TD\n" +
            "SqEuVBaehN+Ul31xbMKEn9OjIf6qmvc2exMnzoNJ2mHgMC2XEFtW1BB6SeC9/yfD\n" +
            "mLPhAgMBAAGjggOMMIIDiDAfBgNVHSMEGDAWgBTwnIX9op99j8lou9XUiU0dvtOQ\n" +
            "/zAdBgNVHQ4EFgQUsbdc8AYBdr5O2IG0YC3szXVpXNwwHQYDVR0RBBYwFIIJKi5l\n" +
            "bXF4LmlvggdlbXF4LmlvMD4GA1UdIAQ3MDUwMwYGZ4EMAQIBMCkwJwYIKwYBBQUH\n" +
            "AgEWG2h0dHA6Ly93d3cuZGlnaWNlcnQuY29tL0NQUzAOBgNVHQ8BAf8EBAMCBaAw\n" +
            "HQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMIGfBgNVHR8EgZcwgZQwSKBG\n" +
            "oESGQmh0dHA6Ly9jcmwzLmRpZ2ljZXJ0LmNvbS9SYXBpZFNTTEdsb2JhbFRMU1JT\n" +
            "QTQwOTZTSEEyNTYyMDIyQ0ExLmNybDBIoEagRIZCaHR0cDovL2NybDQuZGlnaWNl\n" +
            "cnQuY29tL1JhcGlkU1NMR2xvYmFsVExTUlNBNDA5NlNIQTI1NjIwMjJDQTEuY3Js\n" +
            "MIGHBggrBgEFBQcBAQR7MHkwJAYIKwYBBQUHMAGGGGh0dHA6Ly9vY3NwLmRpZ2lj\n" +
            "ZXJ0LmNvbTBRBggrBgEFBQcwAoZFaHR0cDovL2NhY2VydHMuZGlnaWNlcnQuY29t\n" +
            "L1JhcGlkU1NMR2xvYmFsVExTUlNBNDA5NlNIQTI1NjIwMjJDQTEuY3J0MAwGA1Ud\n" +
            "EwEB/wQCMAAwggF8BgorBgEEAdZ5AgQCBIIBbASCAWgBZgB1ABLxTjS9U3JMhAYZ\n" +
            "w48/ehP457Vih4icbTAFhOvlhiY6AAABkSBCuCEAAAQDAEYwRAIgKbe0nHfnjgD7\n" +
            "0qFJV3BxrQ9Huo5tkFjxNxUjVjPn59QCIHPkChtqIxDA4cZASoMVlfv99GYOvTul\n" +
            "J7pwih3FAwc/AHYAfVkeEuF4KnscYWd8Xv340IdcFKBOlZ65Ay/ZDowuebgAAAGR\n" +
            "IEK36QAABAMARzBFAiBduKLMYNm6z06xesyOlTITz6IWomTT0Hv9HTGKao5zOQIh\n" +
            "AP2g7uQPM/8iyhWsvASdmjpDpsoTnqJhdmqbn2Zvf80HAHUA5tIxY0B3jMEQQQbX\n" +
            "cbnOwdJA9paEhvu6hzId/R43jlAAAAGRIEK4AgAABAMARjBEAiAEQ3ll3lApmtdl\n" +
            "hfvIyRh58arJ6nzmnyOrTFahJGTkmAIgYKU241map20aMxSG8vpQyBqiLQq5vPuq\n" +
            "K6+ZljCswrcwDQYJKoZIhvcNAQELBQADggIBAHSuDHvnD7KQE1FGIs1oyXtgppHc\n" +
            "Av/wRoW9Rn5ZQIdQRgee1FhM+dsZUjr80q4VO7GR8TitbuD9aRXVWifPPb56l3nd\n" +
            "DHTURaMnIhpx1YtTpqN5RnvppW9zHJzkVfpmiaO2wejxsa5AA+ZS+5/pryktaCzt\n" +
            "Lvf7yOOsBFr7LD996zC7PNmPPKVGDLNW94C+7znrlFL9Zsxbb+19ExbId0uOouYY\n" +
            "KjD0afK5oagpa3JlTdkq9AUcUHDmIpYGgJsknVg0jdV8vUs5/lDRKES1q84Zxa/1\n" +
            "g4WEDza0fkaOptbQ84/YZHcCwVAinJfrZySucDptJFsRWAWvqOmq3/gYq7dX/Tvz\n" +
            "cwc1z5ua7dO1mwyS9PGbO88eafzBtOmSpU26o2a39KUN92mGHsnzvmQlcdwZN0bO\n" +
            "GYScRZ3Rrhh8k9GD4jo+hGMFvopX7xvZ4IvRJfIGEu6mPgQ+2cQpEBJd0tFixYA/\n" +
            "O8LdxjoVBxi9uzW5h4LSvMH1dB3HCsnO2Bv0RXivIA5gXLxOU8iqfoNrwNB4/vYF\n" +
            "oVStB4cWiTFlV7S2lMvs0vUMl61DhvEsHaejrg2jUXyyvZzQOCOlrXdvrgKBZS9o\n" +
            "Alws788I9+M3hGdKlkD3ob/2YxckomJwoMMXaOn/0daRMeJ0YoE/Sk4FPphPhDd4\n" +
            "fsFD52ghK/hlKPZ9\n" +
            "-----END CERTIFICATE-----\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFyzCCBLOgAwIBAgIQCgWbJfVLPYeUzGYxR3U4ozANBgkqhkiG9w0BAQsFADBh\n" +
            "MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3\n" +
            "d3cuZGlnaWNlcnQuY29tMSAwHgYDVQQDExdEaWdpQ2VydCBHbG9iYWwgUm9vdCBD\n" +
            "QTAeFw0yMjA1MDQwMDAwMDBaFw0zMTExMDkyMzU5NTlaMFwxCzAJBgNVBAYTAlVT\n" +
            "MRcwFQYDVQQKEw5EaWdpQ2VydCwgSW5jLjE0MDIGA1UEAxMrUmFwaWRTU0wgR2xv\n" +
            "YmFsIFRMUyBSU0E0MDk2IFNIQTI1NiAyMDIyIENBMTCCAiIwDQYJKoZIhvcNAQEB\n" +
            "BQADggIPADCCAgoCggIBAKY5PJhwCX2UyBb1nelu9APen53D5+C40T+BOZfSFaB0\n" +
            "v0WJM3BGMsuiHZX2IHtwnjUhLL25d8tgLASaUNHCBNKKUlUGRXGztuDIeXb48d64\n" +
            "k7Gk7u7mMRSrj+yuLSWOKnK6OGKe9+s6oaVIjHXY+QX8p2I2S3uew0bW3BFpkeAr\n" +
            "LBCU25iqeaoLEOGIa09DVojd3qc/RKqr4P11173R+7Ub05YYhuIcSv8e0d7qN1sO\n" +
            "1+lfoNMVfV9WcqPABmOasNJ+ol0hAC2PTgRLy/VZo1L0HRMr6j8cbR7q0nKwdbn4\n" +
            "Ar+ZMgCgCcG9zCMFsuXYl/rqobiyV+8U37dDScAebZTIF/xPEvHcmGi3xxH6g+dT\n" +
            "CjetOjJx8sdXUHKXGXC9ka33q7EzQIYlZISF7EkbT5dZHsO2DOMVLBdP1N1oUp0/\n" +
            "1f6fc8uTDduELoKBRzTTZ6OOBVHeZyFZMMdi6tA5s/jxmb74lqH1+jQ6nTU2/Mma\n" +
            "hGNxUuJpyhUHezgBA6sto5lNeyqc+3Cr5ehFQzUuwNsJaWbDdQk1v7lqRaqOlYjn\n" +
            "iomOl36J5txTs0wL7etCeMRfyPsmc+8HmH77IYVMUOcPJb+0gNuSmAkvf5QXbgPI\n" +
            "Zursn/UYnP9obhNbHc/9LYdQkB7CXyX9mPexnDNO7pggNA2jpbEarLmZGi4grMmf\n" +
            "AgMBAAGjggGCMIIBfjASBgNVHRMBAf8ECDAGAQH/AgEAMB0GA1UdDgQWBBTwnIX9\n" +
            "op99j8lou9XUiU0dvtOQ/zAfBgNVHSMEGDAWgBQD3lA1VtFMu2bwo+IbG8OXsj3R\n" +
            "VTAOBgNVHQ8BAf8EBAMCAYYwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMC\n" +
            "MHYGCCsGAQUFBwEBBGowaDAkBggrBgEFBQcwAYYYaHR0cDovL29jc3AuZGlnaWNl\n" +
            "cnQuY29tMEAGCCsGAQUFBzAChjRodHRwOi8vY2FjZXJ0cy5kaWdpY2VydC5jb20v\n" +
            "RGlnaUNlcnRHbG9iYWxSb290Q0EuY3J0MEIGA1UdHwQ7MDkwN6A1oDOGMWh0dHA6\n" +
            "Ly9jcmwzLmRpZ2ljZXJ0LmNvbS9EaWdpQ2VydEdsb2JhbFJvb3RDQS5jcmwwPQYD\n" +
            "VR0gBDYwNDALBglghkgBhv1sAgEwBwYFZ4EMAQEwCAYGZ4EMAQIBMAgGBmeBDAEC\n" +
            "AjAIBgZngQwBAgMwDQYJKoZIhvcNAQELBQADggEBAAfjh/s1f5dDdfm0sNm74/dW\n" +
            "MbbsxfYV1LoTpFt+3MSUWvSbiPQfUkoV57b5rutRJvnPP9mSlpFwcZ3e1nSUbi2o\n" +
            "ITGA7RCOj23I1F4zk0YJm42qAwJIqOVenR3XtyQ2VR82qhC6xslxtNf7f2Ndx2G7\n" +
            "Mem4wpFhyPDT2P6UJ2MnrD+FC//ZKH5/ERo96ghz8VqNlmL5RXo8Ks9rMr/Ad9xw\n" +
            "Y4hyRvAz5920myUffwdUqc0SvPlFnahsZg15uT5HkK48tHR0TLuLH8aRpzh4KJ/Y\n" +
            "p0sARNb+9i1R4Fg5zPNvHs2BbIve0vkwxAy+R4727qYzl3027w9jEFC6HMXRaDc=\n" +
            "-----END CERTIFICATE-----\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIDrzCCApegAwIBAgIQCDvgVpBCRrGhdWrJWZHHSjANBgkqhkiG9w0BAQUFADBh\n" +
            "MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3\n" +
            "d3cuZGlnaWNlcnQuY29tMSAwHgYDVQQDExdEaWdpQ2VydCBHbG9iYWwgUm9vdCBD\n" +
            "QTAeFw0wNjExMTAwMDAwMDBaFw0zMTExMTAwMDAwMDBaMGExCzAJBgNVBAYTAlVT\n" +
            "MRUwEwYDVQQKEwxEaWdpQ2VydCBJbmMxGTAXBgNVBAsTEHd3dy5kaWdpY2VydC5j\n" +
            "b20xIDAeBgNVBAMTF0RpZ2lDZXJ0IEdsb2JhbCBSb290IENBMIIBIjANBgkqhkiG\n" +
            "9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4jvhEXLeqKTTo1eqUKKPC3eQyaKl7hLOllsB\n" +
            "CSDMAZOnTjC3U/dDxGkAV53ijSLdhwZAAIEJzs4bg7/fzTtxRuLWZscFs3YnFo97\n" +
            "nh6Vfe63SKMI2tavegw5BmV/Sl0fvBf4q77uKNd0f3p4mVmFaG5cIzJLv07A6Fpt\n" +
            "43C/dxC//AH2hdmoRBBYMql1GNXRor5H4idq9Joz+EkIYIvUX7Q6hL+hqkpMfT7P\n" +
            "T19sdl6gSzeRntwi5m3OFBqOasv+zbMUZBfHWymeMr/y7vrTC0LUq7dBMtoM1O/4\n" +
            "gdW7jVg/tRvoSSiicNoxBN33shbyTApOB6jtSj1etX+jkMOvJwIDAQABo2MwYTAO\n" +
            "BgNVHQ8BAf8EBAMCAYYwDwYDVR0TAQH/BAUwAwEB/zAdBgNVHQ4EFgQUA95QNVbR\n" +
            "TLtm8KPiGxvDl7I90VUwHwYDVR0jBBgwFoAUA95QNVbRTLtm8KPiGxvDl7I90VUw\n" +
            "DQYJKoZIhvcNAQEFBQADggEBAMucN6pIExIK+t1EnE9SsPTfrgT1eXkIoyQY/Esr\n" +
            "hMAtudXH/vTBH1jLuG2cenTnmCmrEbXjcKChzUyImZOMkXDiqw8cvpOp/2PV5Adg\n" +
            "06O/nVsJ8dWO41P0jmP6P6fbtGbfYmbW0W5BjfIttep3Sp+dWOIrWcBAI+0tKIJF\n" +
            "PnlUkiaY4IBIqDfv8NZ5YBberOgOzW6sRBc4L0na4UU+Krk2U886UAb3LujEV0ls\n" +
            "YSEY1QSteDwsOoBrp+uvFRTp2InBuThs4pFsiv9kuXclVzDAGySj4dzp30d8tbQk\n" +
            "CAUw7C29C79Fv1C5qfPrmAESrciIxpg0X40KPMbp1ZWVbd4=\n" +
            "-----END CERTIFICATE-----\n";

    private final String TAG = "SSLUtil";
    private final boolean enableLogging=false;

    private SimpleLogger logger = new SimpleLogger();

    private void log(String data){
        if (enableLogging){
            logger.log(data);
        }
    }
    public SslContext getEMQSslContext(){
        SslContext retval =null;
        try {
            retval = createClientContext(emqPEMchain);
        }catch (Exception e){
            log("An error occurred getting the SslContext");
        }
        return retval;
    }

    public  SslContext createClientContext(String certPEM) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        final String delimiter = "-----END CERTIFICATE-----";
        // Create key store
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        String[] certs = certPEM.split(delimiter);
//        for (int i = 0; i < certs.length; i++) {
//            log("cert " + Integer.toString(i));
//            log(certs[i]  + delimiter);
//            log("======================================");
//
//        }
        if (certs.length>0) {
            for (int i = 0; i < certs.length - 1; i++) {
                String certData = certs[i] + delimiter;
                InputStream pemStream = new ByteArrayInputStream(certData.getBytes());

                // Create certificate
                Certificate certificate = CertificateFactory.getInstance("X.509")
                        .generateCertificate(pemStream);
                keyStore.setCertificateEntry("cert" + Integer.toString(i), certificate);
            }


            // Create trust factory
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

            trustManagerFactory.init(keyStore);

            return SslContextBuilder.forClient()
                    .trustManager(trustManagerFactory)
                    .build();
        }
        return null;
    }



}
