"""Gerar QR Codes e cartaz A4: python -m pip install 'qrcode[pil]'."""
from pathlib import Path
import qrcode
import qrcode.image.svg
from PIL import Image, ImageDraw, ImageFont

out = Path(__file__).resolve().parent
urls = {
    'qr-android': 'https://github.com/MatheoDavalos/controle-robo-android-iphone/raw/refs/heads/main/apk/Controle_Robo.apk',
    'qr-wifi': 'WIFI:T:WPA;S:RobotUA-Controle;P:controle123;;',
    'qr-controle-wifi': 'http://192.168.4.1',
    'qr-repositorio': 'https://github.com/MatheoDavalos/controle-robo-android-iphone',
}
codes = {}
for name, payload in urls.items():
    qr = qrcode.QRCode(error_correction=qrcode.constants.ERROR_CORRECT_M, box_size=12, border=4)
    qr.add_data(payload)
    qr.make(fit=True)
    codes[name] = qr.make_image().convert('RGB')
    codes[name].save(out / (name + '.png'))
    qr.make_image(image_factory=qrcode.image.svg.SvgPathImage).save(out / (name + '.svg'))

im = Image.new('RGB', (2480, 3508), '#f5f7fb')
d = ImageDraw.Draw(im)
def text(x, y, message, size=48, bold=False, color='#142137'):
    suffix = '-Bold' if bold else ''
    font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans'+suffix+'.ttf', size)
    d.text((x,y), message, font=font, fill=color)
def paste_qr(name, x, y, limit):
    img = codes[name]
    modules = img.width // 12
    side = modules * (limit // modules)
    img = img.resize((side, side), Image.Resampling.NEAREST)
    im.paste(img, (x+(limit-side)//2, y+(limit-side)//2))
d.rectangle((0,0,2480,420), fill='#10233e')
text(140,85,'CONTROLE O ROBÔ',112,True,'white')
text(145,245,'Android + iPhone',58,False,'#b9dcff')
text(150,500,'ANDROID',76,True)
text(150,610,'Baixe o aplicativo e use Bluetooth.',50)
paste_qr('qr-android',155,705,740)
text(1020,790,'1. Leia o QR com internet.',48,True)
text(1020,905,'2. Instale Controle_Robo.',48)
text(1020,1020,'3. Pareie com RobotUA.',48)
text(1020,1135,'4. Conecte pelo aplicativo.',48)
text(1020,1320,'Android 8.0 ou superior',42,False,'#50617b')
d.line((150,1530,2330,1530),fill='#cad3df',width=4)
text(150,1600,'iPHONE / WI-FI',76,True)
text(150,1710,'Sem instalar aplicativo. Faça os dois passos:',48)
text(215,1830,'1. ENTRE NO WI-FI',52,True)
text(1380,1830,'2. ABRA O CONTROLE',52,True)
paste_qr('qr-wifi',180,1930,800)
paste_qr('qr-controle-wifi',1350,1930,800)
text(215,2780,'Rede: RobotUA-Controle',43,True)
text(215,2850,'Senha: controle123',43)
text(1380,2780,'Safari: http://192.168.4.1',43,True)
text(1380,2850,'Conecte ao Wi-Fi primeiro.',43)
text(150,3040,'A rede do robô não precisa de internet.',48,True)
text(150,3140,'Combine sua vez com o responsável. Um celular por vez.',43)
text(150,3300,'github.com/MatheoDavalos/controle-robo-android-iphone',38,False,'#50617b')
im.save(out / 'cartaz-feira.png', dpi=(300,300))
im.resize((707,1000)).save(out / 'cartaz-preview.png')
