# Proyek Sistem Reservasi dan Manajemen Hotel "Griya Bougenville"

## Pendahuluan

Repositori ini berisi kode sumber untuk proyek akhir mata kuliah **Pemrograman Berorientasi Objek (PBO)**. Proyek ini mengimplementasikan sebuah sistem informasi manajemen hotel dalam bentuk aplikasi desktop. Tujuan utama dari sistem ini adalah untuk menyediakan solusi digital yang terintegrasi guna menggantikan proses operasional manual pada hotel skala kecil hingga menengah. Aplikasi ini mencakup fungsionalitas inti seperti reservasi, manajemen kamar, serta proses *check-in* dan *check-out*, yang dirancang untuk meningkatkan efisiensi dan akurasi data.

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

## Tumpukan Teknologi (Technological Stack)

Pengembangan sistem ini didukung oleh serangkaian teknologi berikut:
* **Bahasa Pemrograman**: **Java Development Kit (JDK) 21**.
* **Framework Antarmuka**: **JavaFX**, digunakan untuk membangun antarmuka pengguna yang modern dan responsif.
* **Sistem Manajemen Basis Data**: **PostgreSQL**, sebuah sistem basis data relasional objek yang kuat untuk persistensi data.
* **Alat Manajemen Proyek dan Dependensi**: **Apache Maven**, digunakan untuk mengelola dependensi proyek (seperti *driver* JDBC) dan siklus hidup pembangunan (*build lifecycle*).
* **Lingkungan Pengembangan Terintegrasi (IDE)**: **IntelliJ IDEA**.

---

## Struktur Proyek dan Komponen Pendukung

* **`pom.xml`**: Merupakan berkas *Project Object Model* (POM) untuk Maven. Berkas ini mendefinisikan konfigurasi proyek, dependensi eksternal, dan *plugin* yang diperlukan untuk proses kompilasi dan pengemasan.
* **`mvnw` & `.mvn/wrapper`**: Komponen **Maven Wrapper**. Ini adalah skrip yang memungkinkan proyek untuk dibangun menggunakan versi Maven yang telah ditentukan tanpa memerlukan instalasi Maven secara manual di lingkungan lokal. Hal ini menjamin konsistensi proses *build* di berbagai mesin pengembangan.
* **`.gitignore`**: Berkas konfigurasi untuk Git yang secara eksplisit mendefinisikan file dan direktori yang harus diabaikan dari *version control*. Ini penting untuk menjaga kebersihan repositori dengan mengecualikan file yang dihasilkan oleh IDE atau proses *build*.
