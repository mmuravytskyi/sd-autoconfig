# Software-Defined Autoconfiguration

SDA wykorzystuje koncepcję sieci SDN do automatyzacji konfigurowania i wdrażania przełączników w sieci. Funkcjonalnie jest to podobne do Ansible’a, ale bardziej skalowalne i zintegrowane z siecią SDN. SDA stanowi część sterownika **OpenFlow** i korzysta z protokołu **Netconf**. Działa SDA zgodnie z następującym flow:

  * **Detekcja nowych urządzeń w topologii** \
SDA dowiaduje się o pojawieniu się nowego urządzenia w sieci poprzez nasłuchiwanie wiadomości `OF_HELLO`, które są wysyłane podczas nawiązywania połączenia pomiędzy przełącznikiem a sterownikiem. 
  * **Weryfikacja w Asset DB** \
  SDA, na podstawie adresu MAC pochodzącego z wiadomości OF_HELLO, weryfikuje przełącznik. Informacje o zaufanych przełącznikach, ich roli w sieci oraz dane do konfiguracji są przechowywane w bazie danych Assets.
  * **Odcięcie dostępu jeżeli urządzenie jest nieautoryzowane** \
  Jeśli urządzenie nie przechodzi walidacji w AssetDB, SDA domyślnie odetnie to urządzenie od reszty sieci, odpowiednio konfigurując przełączniki sąsiadujące. Wszystkie przepływy pochodzące od tego urządzenia będą DROP’owane.
  * **Przygotowanie konfiguracji** \
  W przypadku udanej weryfikacji SDA generuje konfiguracje dla przełącznika na podstawie danych zawartych w AssetDB oraz pozycji przełącznika w sieci.
  * **Konfiguracja urządzenia** \
  Przygotowana konfiguracja jest wysyłana do przełącznika wykorzystując protokół Netconf. 

[image]

### Założenia: 
  * Nowy przełącznik ma domyślnie skonfigurowany adres sterownika oraz włączony netconf;
  * Przełączniki wspierają Openflow i Netconf; 
  * Dane o przełącznikach w bazie AssetDB są dostępne przed dołączeniem przełącznika do sieci;

### Scenariusz zastosowania:
  SDA najlepiej się sprawdza w funkcjonowaniu na skalę - kiedy liczba urządzeń w sieci znacząco przekracza możliwości inżynierów opiekujących się tą siecią np. centra danych. Zamówienia sprzętu do CD są dokładnie udokumentowane, na podstawie czego może powstawać baza AssetDB. Topologia w CD jest znana, a relacje urządzeń zdeterminowane, co pozwala generować konfiguracje na bieżąco. 

### Problemy na etapach realizacji: 
  * **OVS + OF-Config + OpenDayLight/OpenFloodLight/Ryu** \
  [OF-Config](https://github.com/openvswitch/of-config) udostępnia interfejs Netconf’owy do konfigurowania OVS’ów. Pozwala on pobierać bieżącą konfigurację, ale aktywnie edytować konfiguracje można tylko dla nieaktywnych OVS’ów. Przetestowaliśmy na tym etapie 3 sterowniki i żaden nie dawał pożądanych wyników.
  * **vRouter + Netconf** \
  OVS próbowaliśmy zastąpić otwartoźródłowym routerem, żaden z przetestowanych ([DanOS](https://www.danosproject.org), … ) nie spełniał założeń o Netconf’ie i Openflow.
  * **Lab Juniper MX80 + OpenFloodLight + Netconf** \
  Urządzenia w laboratorium spełniały wszystkie założenia i sprawdziły się w *proof-of-concept*.
  
### Co udało nam się zrobić?
  Zrobiliśmy *proof-of-concept* dla SDA. Dodaliśmy prostą logikę w sterowniku, która konfiguruje serwer FTP na routerze w przypadku kiedy pojawia się wiadomość SWITCH_UPDATE od tego routera. Konfiguracja odbywa się za pomocą Netconfa i polega na wysłaniu przygotowanego xml’a. Routery testowe zostały skonfigurowane zgodnie z założeniami - włączony Netconf oraz skonfigurowany adres sterownika.
  
  
### Jak to uruchomić?
Wchodzimy na Pluton’a:
```
ssh <USER>@pluton.kt.agh.edu.pl
```
Logujemy się na router R42:
```
ssh lab@10.80.0.42
```
Bieżącą konfigurację możemy sprawdzić:
```
show configuration
```
W ramach proof-of-concept będziemy konfigurować FTP na routerze, więc wyczyścimy konfigurację FTP:
```
configure
delete system services ftp
commit
```
W nowej sesji wchodzimy na VM na której znajduje się sterownik, komendę wykonujemy na pluton’ie:
```
ssh lab@10.80.0.22
```
Sterownik jest skomplikowany, wystarczy że uruchomimy:
```
cd floodlight
java -jar target/floodlight.jar
```
Następujące logi oznaczają, że router został skonfigurowany:
```
[k.a.d.SdnLabTopologyListener:Scheduled-3] config <configuration>
    <system>
        <services>
            <ftp/>
        </services>
    </system>
</configuration>
```
Możemy też to sprawdzić na routerze: 
```
show configuration
```
```
…services {
        ftp;
        ssh;
        telnet;
        netconf {                       
            ssh {
                port 830;
            }
            traceoptions {
                file netconf.log;
            …
```
