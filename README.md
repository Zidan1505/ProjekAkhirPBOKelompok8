# Proyek Sistem Reservasi dan Manajemen Hotel "Griya Bougenville"

## Pendahuluan

Repositori ini berisi kode sumber untuk proyek akhir mata kuliah **Pemrograman Berorientasi Objek (PBO)**. Proyek ini mengimplementasikan sebuah sistem informasi manajemen hotel dalam bentuk aplikasi desktop. Tujuan utama dari sistem ini adalah untuk menyediakan solusi digital yang terintegrasi guna menggantikan proses operasional manual pada hotel skala kecil hingga menengah. Aplikasi ini mencakup fungsionalitas inti seperti reservasi, manajemen kamar, serta proses *check-in* dan *check-out*, yang dirancang untuk meningkatkan efisiensi dan akurasi data.

---

## Technological Stack

Pengembangan sistem ini didukung oleh serangkaian teknologi berikut:
* **Bahasa Pemrograman**: **Java Development Kit (JDK) 21**.
* **Framework Antarmuka**: **JavaFX**, digunakan untuk membangun antarmuka pengguna yang modern dan responsif.
* **Sistem Manajemen Basis Data**: **PostgreSQL**, sebuah sistem basis data relasional objek yang kuat untuk persistensi data.
* **Alat Manajemen Proyek dan Dependensi**: **Apache Maven**, digunakan untuk mengelola dependensi proyek (seperti *driver* JDBC) dan siklus hidup pembangunan (*build lifecycle*).
* **Lingkungan Pengembangan Terintegrasi (IDE)**: **IntelliJ IDEA**.

---

## Arsitektur Perangkat Lunak

Sistem ini dirancang dengan mengadopsi pola arsitektur **Model-View-Controller (MVC)**. Pemilihan pola ini didasarkan pada kemampuannya untuk memisahkan antara representasi data (Model), antarmuka pengguna (View), dan logika kontrol (Controller). Pendekatan ini menghasilkan sistem yang modular, memfasilitasi pemeliharaan kode (*maintainability*), dan memungkinkan pengembangan paralel pada setiap komponen.

### 1. Model
Komponen Model merepresentasikan data dan logika bisnis aplikasi. Komponen ini bertanggung jawab atas interaksi langsung dengan basis data dan menegakkan aturan bisnis.
* **Kelas Entitas**: Merupakan representasi objek dari tabel-tabel dalam basis data, seperti `Room.java`, `Reservation.java`, `Employee.java`, dan `HousekeepingRecord.java`. Setiap kelas entitas membungkus atribut data beserta metode *accessor* dan *mutator* (getters/setters).
* **`DatabaseConnection.java`**: Kelas ini mengimplementasikan **Singleton Pattern** untuk mengelola konektivitas ke basis data PostgreSQL. Pola ini memastikan bahwa hanya satu instans koneksi yang dibuat dan digunakan secara global di seluruh siklus hidup aplikasi, sehingga mengoptimalkan penggunaan sumber daya.

### 2. View
Komponen View bertanggung jawab penuh atas presentasi data dan antarmuka pengguna (GUI).
* **Berkas FXML (`/src/main/resources/org/example/hotelsystem/`)**: Struktur dan tata letak dari setiap antarmuka didefinisikan dalam berkas FXML, seperti `Administrator.fxml`, `Login.fxml`, dan `GuestView.fxml`. Penggunaan FXML memungkinkan pemisahan desain antarmuka dari logika aplikasi.
* **Cascading Style Sheets (CSS)**: Gaya visual dari komponen-komponen antarmuka diatur melalui berkas CSS eksternal. Hal ini memungkinkan kustomisasi tampilan yang konsisten dan modern tanpa mengubah kode sumber logika.

### 3. Controller
Komponen Controller berfungsi sebagai perantara antara Model dan View.
* **Kelas Controller (`/src/main/java/org/example/hotelsystem/`)**: Untuk setiap berkas FXML, terdapat kelas Controller yang sesuai (misalnya, `AdministratorController.java`). Kelas ini menangani *event* yang dipicu oleh interaksi pengguna (mis. klik tombol), memproses input, berkomunikasi dengan Model untuk mengambil atau memanipulasi data, dan kemudian memperbarui View untuk menampilkan hasilnya.

---

### Skema Database

Skema database untuk Sistem Informasi Hotel ini dirancang menggunakan PostgreSQL dan terdiri dari lima tabel utama serta tiga objek sequence pendukung yang saling berhubungan untuk mengelola data operasional hotel. Kelima tabel tersebut adalah employee, room, reservation, housekeeping_record, dan app_counters. Hubungan antar tabel diatur menggunakan foreign key constraint untuk memastikan integritas dan konsistensi data.

#### **a. Tabel employee**

Tabel ini berfungsi sebagai pusat data untuk semua karyawan hotel yang memiliki akses ke sistem.

* *Tujuan*: Menyimpan informasi login, data pribadi, dan status kerja dari setiap karyawan.
* *Struktur Kolom*:

| Nama Kolom | Tipe Data | Constraint | Deskripsi |
| :--- | :--- | :--- | :--- |
| id | INTEGER | PRIMARY KEY, IDENTITY | Nomor identifikasi unik untuk setiap karyawan. |
| username | VARCHAR(20) | NOT NULL, UNIQUE | Nama pengguna yang digunakan untuk login. |
| user_password| VARCHAR | NOT NULL | Kata sandi untuk login. |
| fullname | VARCHAR(50) | NOT NULL | Nama lengkap karyawan. |
| email | VARCHAR(30) | NOT NULL, UNIQUE | Alamat email unik karyawan. |
| contact | VARCHAR(30) | NOT NULL, UNIQUE | Nomor kontak unik karyawan. |
| status_employee| VARCHAR(20) | NOT NULL, CHECK | Status login saat ini (Logged-In atau Logged-Out). |

#### **b. Tabel room**

Tabel ini adalah data master untuk semua kamar yang dimiliki oleh hotel.

* *Tujuan*: Menyimpan semua atribut fisik dan status dari setiap kamar.
* *Struktur Kolom*:

| Nama Kolom | Tipe Data | Constraint | Deskripsi |
| :--- | :--- | :--- | :--- |
| room_number | INTEGER | PRIMARY KEY | Nomor unik kamar. |
| status | VARCHAR(20) | NOT NULL, CHECK | Status operasional kamar (available, not available, maintenance). |
| price | REAL | NOT NULL | Harga sewa per malam. |
| ac, single_bed, twin_bed, double_bed | BOOLEAN | NOT NULL | Menandakan ada/tidaknya fasilitas. |
| last_change_by_admin_id | INTEGER | FOREIGN KEY | ID Admin terakhir yang mengubah data kamar ini. |
| last_modified_date | DATE | - | Tanggal terakhir data kamar ini diubah. |
| check_status | VARCHAR(30) | NOT NULL, CHECK | Status siklus hunian (Waiting for Check-In, Check-In, dll.). |

#### **c. Tabel reservation**

Tabel ini mencatat semua transaksi pemesanan kamar.

* *Tujuan*: Menyimpan histori dan data terkini dari setiap pemesanan.
* *Struktur Kolom*:

| Nama Kolom | Tipe Data | Constraint | Deskripsi |
| :--- | :--- | :--- | :--- |
| reservation_id | INTEGER | PRIMARY KEY, IDENTITY | ID unik untuk setiap transaksi reservasi. |
| receptionist_id | INTEGER | FOREIGN KEY | ID Resepsionis yang mengonfirmasi check-in/out. |
| room_number | INTEGER | FOREIGN KEY| Nomor kamar yang dipesan. |
| guest_name | VARCHAR | NOT NULL | Nama lengkap tamu. |
| guest_contact| VARCHAR | NOT NULL | Nomor kontak tamu. |
| reservation_date| DATE | NOT NULL | Tanggal saat pemesanan dibuat. |
| check_in_date| DATE | NOT NULL | Tanggal mulai menginap. |
| check_out_date| DATE | NOT NULL | Tanggal selesai menginap. |
| reservation_status|VARCHAR(30)| NOT NULL, DEFAULT 'CONFIRMED' | Status siklus reservasi (CONFIRMED, CHECKED_IN, COMPLETED). |

#### **d. Tabel housekeeping_record**

Tabel ini digunakan oleh petugas kebersihan untuk mencatat aktivitas mereka.

* *Tujuan*: Mencatat semua laporan dari petugas kebersihan terkait kondisi kamar.
* *Struktur Kolom*:

| Nama Kolom | Tipe Data | Constraint | Deskripsi |
| :--- | :--- | :--- | :--- |
| record_id | INTEGER | PRIMARY KEY, IDENTITY | ID unik untuk setiap catatan. |
| housekeeping_id | INTEGER | FOREIGN KEY | ID Petugas Kebersihan yang membuat catatan. |
| room_number | INTEGER | FOREIGN KEY| Nomor kamar yang dicatat. |
| record_date | DATE | NOT NULL | Tanggal saat catatan dibuat. |
| notes | VARCHAR(500) | - | Isi catatan atau laporan dari petugas. |

#### **e. Tabel app_counters**

Tabel utilitas untuk menyimpan nilai penghitung yang digunakan oleh aplikasi.

* *Tujuan*: Menyimpan nilai variabel global, seperti kode unik pembayaran.
* *Struktur Kolom*:

| Nama Kolom | Tipe Data | Constraint | Deskripsi |
| :--- | :--- | :--- | :--- |
| counter_name | VARCHAR(50) | PRIMARY KEY | Nama unik dari penghitung. |
| current_value| INTEGER | NOT NULL | Nilai terakhir dari penghitung. |

#### **f. Objek Sequence**
Skema ini juga memanfaatkan tiga objek sequence yang dibuat secara otomatis oleh PostgreSQL untuk kolom IDENTITY. Sequence adalah objek khusus yang menghasilkan urutan angka unik.

* **public.employee_id_seq**: Terhubung ke kolom id di tabel employee.
* **public.reservation_reservation_id_seq**: Terhubung ke kolom reservation_id di tabel reservation.
* **public.housekeeping_record_record_id_seq**: Terhubung ke kolom record_id di tabel housekeeping_record.

Penggunaan sequence ini mengotomatiskan proses pembuatan ID, mengurangi risiko kesalahan, dan memastikan integritas data di seluruh sistem.

---

## Struktur Proyek dan Komponen Pendukung

* **`pom.xml`**: Merupakan berkas *Project Object Model* (POM) untuk Maven. Berkas ini mendefinisikan konfigurasi proyek, dependensi eksternal, dan *plugin* yang diperlukan untuk proses kompilasi dan pengemasan.
* **`mvnw` & `.mvn/wrapper`**: Komponen **Maven Wrapper**. Ini adalah skrip yang memungkinkan proyek untuk dibangun menggunakan versi Maven yang telah ditentukan tanpa memerlukan instalasi Maven secara manual di lingkungan lokal. Hal ini menjamin konsistensi proses *build* di berbagai mesin pengembangan.
* **`.gitignore`**: Berkas konfigurasi untuk Git yang secara eksplisit mendefinisikan file dan direktori yang harus diabaikan dari *version control*. Ini penting untuk menjaga kebersihan repositori dengan mengecualikan file yang dihasilkan oleh IDE atau proses *build*.
