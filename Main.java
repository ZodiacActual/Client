import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;

public class Main {

    public static byte[] RSAEncrypt(PublicKey pubkey,SecretKey secretkey)
            throws GeneralSecurityException{
        byte[] secretkeybyte = secretkey.getEncoded();
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubkey);
        byte[] encryptdata = cipher.doFinal(secretkeybyte);
        return encryptdata;
    }

    public static byte[] RSADecrypt(PrivateKey prikey, byte[] encryptedSecretkeybyte)
            throws GeneralSecurityException{
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, prikey);
        byte[] secretkeybyte = cipher.doFinal(encryptedSecretkeybyte);
        return secretkeybyte;
    }

    //대칭키 암호화(AES)
    public static byte[] AESencrypt(SecretKey secretKey, byte[] plainData) throws GeneralSecurityException {

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptData = cipher.doFinal(plainData);
        return encryptData;
    }

    // 대칭키 복호화(AES)
    public static byte[] AESdecrypt(SecretKey secretKey, byte[] encryptData) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] plainData = cipher.doFinal(encryptData);
        return plainData;
    }

    public static boolean isStringDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        boolean end = false;
        boolean Loop = true;

        try {
            Socket socket = new Socket("localhost", 9999);
            Scanner scan = new Scanner(System.in);
            InputStream is = socket.getInputStream();
            final ObjectInputStream ois = new ObjectInputStream(is);
            OutputStream os = socket.getOutputStream();
            final ObjectOutputStream oos = new ObjectOutputStream(os);
            System.out.println("서버에 연결중입니다.....");
            String outputmessage;
            byte[] buffer;

            //공개키쌍 생성
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            KeyPair keypair = kpg.genKeyPair();
            final PublicKey pubKey = keypair.getPublic();
            final PrivateKey priKey = keypair.getPrivate();

            //여기서부터 평문전송
            //서버 공개키 수신 및 복구
            buffer = (byte[])ois.readObject();
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey serverkey = kf.generatePublic(new X509EncodedKeySpec(buffer));

            //클라이언트 공개키 전송
            oos.writeObject(pubKey.getEncoded());
            oos.flush();

            //여기서부터 공개키로 암호화 전송
            //비밀키 수신
            buffer = (byte[])ois.readObject();
            byte[] Skeybyte = RSADecrypt(priKey, buffer);
            SecretKey Skey = new SecretKeySpec(Skeybyte, "AES");

            //검증을 위한 비밀키 전송
            oos.writeObject(RSAEncrypt(serverkey, Skey));
            oos.flush();
            boolean veri = (boolean)ois.readObject();

            if(veri == true) {
                System.out.println("키 교환 성공");
            }
            else {
                System.out.println("키 교환 실패");
                System.exit(0);
            }

            for (; ; ) {
                buffer = (byte[])ois.readObject();
                String inputmessage = new String(AESdecrypt(Skey, buffer));
                if(isStringDouble(inputmessage)) {
                    if(Integer.parseInt(inputmessage) == 1) {//1일때 입력값을 받고 서버에 전송
                        System.out.print("입력 : ");
                        outputmessage = scan.nextLine();
                        oos.writeObject(AESencrypt(Skey, outputmessage.getBytes()));
                        oos.flush();
                    } else if(Integer.parseInt(inputmessage) == 2) {
                        System.out.println("프로그램을 종료합니다. ");
                        end = true;
                    }
                }
                else {
                    //일반 문자열
                    System.out.println(inputmessage);
                }
            }
        } catch(UnknownHostException e ) {
            e.printStackTrace();
        }
        catch(IOException |ClassNotFoundException | GeneralSecurityException ex) {
            ex.printStackTrace();
        }
    }
}
