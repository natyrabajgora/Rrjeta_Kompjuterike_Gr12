# 📡 UDP File Server — Projekti i Grupit 12

Ky projekt implementon një **UDP File Server** me dy lloje klientësh (**ADMIN** dhe **READ_ONLY**) duke përmbushur plotësisht kërkesat e lëndës *Rrjeta Kompjuterike*.

---

## 👥 Grupi 12
- Natyra Bajgora 
- Vesa Hadergjonaj
- Erion Troni
- Leon Troni

---

# 🖥️ Serveri — Përmbledhje

### ✔ 1. Porti dhe IP adresa  
Serveri punon në:
- **IP:** 127.0.0.1  
- **PORT:** 5000  

### ✔ 2. Pranimi i shumë klientëve  
Serveri pranon deri në **10 klientë aktivë**.  
Mbi kufi → refuzon lidhjet me mesazhin: SERVER BUSY: Too many active clients.

### ✔ 3. Menaxhimi i kërkesave  
Çdo klient dërgon komanda UDP dhe serveri i përpunon ato përmes:
- `ClientSession`
- `FileCommandHandler`
- `ThreadPool` (për performancë më të mirë)

### ✔ 4. Ruajtja e mesazheve  
Të gjitha mesazhet regjistrohen në: logs/messages.log

### ✔ 5. Timeout i klientëve joaktivë  
Nëse klienti nuk dërgon mesazhe për **20 sekonda**, ai largohet automatikisht.  
Nëse lidhet sërish → rigjenerohet sesioni.

### ✔ 6. Qasje e plotë për ADMIN  
Identifikimi bëhet me:
HELLO client1 ADMIN
HELLO client2 READ

Admin ka qasje të plotë (read/write/delete/upload/download).

### ✔ 7. Monitorim trafiku në kohë reale  
Komanda: STATS

Tregon:
- numrin e lidhjeve aktive  
- IP-të e klientëve  
- mesazhet për klient  
- bytes received/sent  
- total trafikut  

Statistikat ruhen edhe te:

# 📁 Komandat e ADMIN-it

| Komanda | Përshkrimi |
|--------|------------|
| `/list` | Liston file-t e serverit |
| `/read <file>` | Lexon përmbajtjen e një file-i |
| `/upload <file>` | Ngarkon file në server (Base64) |
| `/download <file>` | Shkarkon file nga serveri |
| `/delete <file>` | Fshin file |
| `/search <keyword>` | Kërkon në emrat e file-ve |
| `/info <file>` | Shfaq madhësinë & datat e file-it |

---

# 👥 Klientët

## 🔹 AdminClient
- write(), read(), execute()
- qasje e plotë në `server_files/`
- komanda më të shpejta (prioritet)

## 🔹 ReadOnlyClient
Lejohet vetëm:
/list
/read <file>
/search <keyword>

# 🔌 Funksionaliteti i klientit

Klienti:
- krijon socket UDP  
- dërgon tekste te serveri  
- lexon përgjigjet  
- kontrollon rolin (ADMIN/READ_ONLY)  
- ruan portin & IP-në saktë  

---

# 🚀 Ekzekutimi

### Nis serverin: java server.UDPServer

### Nis AdminClient: java client.AdminClient 1

### Nis ReadOnlyClient: java client.ReadOnlyClient 2

---

# ✅ Projekti i përmbush të gjitha kërkesat:

✔ Port + IP të definuara  
✔ Pranimi i shumë klientëve  
✔ Refuzim i klientëve të tepërt  
✔ Menaxhim i kërkesave  
✔ Ruajtje mesazhesh (logs)  
✔ Timeout + rikuperim  
✔ Klient me qasje të plotë  
✔ Monitorim trafiku + STATS  
✔ Komanda të file-management  
✔ Dërgim/përgjigje tekstuale  
✔ Diferencim ADMIN vs READ_ONLY  
✔ Prioritet i shpejtësisë për ADMIN  

---
