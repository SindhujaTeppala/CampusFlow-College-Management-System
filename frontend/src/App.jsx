import React, { useEffect, useState } from 'react'
import { BookOpen, Users, GraduationCap, LayoutDashboard, LogOut, Plus, Search, Trash2, Pencil, X, CheckCircle2 } from 'lucide-react'
import api from './api'

const emptyCourse = { title:'', code:'', description:'', credits:3, capacity:60 }
const emptyStudent = { name:'', email:'', password:'', rollNumber:'', department:'CSE', phone:'' }

export default function App() {
  const [user,setUser] = useState(() => JSON.parse(localStorage.getItem('college_user') || 'null'))
  const [login,setLogin] = useState({email:'',password:''})
  const [error,setError] = useState('')
  const [notice,setNotice] = useState('')
  const [tab,setTab] = useState('dashboard')
  const [summary,setSummary] = useState({})
  const [courses,setCourses] = useState([])
  const [students,setStudents] = useState([])
  const [teachers,setTeachers] = useState([])
  const [enrollments,setEnrollments] = useState([])
  const [selectedCourse,setSelectedCourse] = useState(null)
  const [search,setSearch] = useState('')
  const [courseForm,setCourseForm] = useState(emptyCourse)
  const [studentForm,setStudentForm] = useState(emptyStudent)
  const [teacherForm,setTeacherForm] = useState({name:'',email:'',password:'',department:'CSE',phone:''})
  const [editingCourse,setEditingCourse] = useState(null)
  const [editingStudent,setEditingStudent] = useState(null)
  const [busy,setBusy] = useState(false)

  const role = user?.role
  const isAdmin = role === 'ADMIN'
  const isTeacher = role === 'TEACHER'
  const isStudent = role === 'STUDENT'

  async function loadAll() {
    if (!user) return
    try {
      const [s,c] = await Promise.all([api.get('/dashboard/summary'),api.get('/courses')])
      setSummary(s.data); setCourses(c.data)
      if (isAdmin || isTeacher) {
        const st = await api.get('/students'); setStudents(st.data)
      }
      if (isAdmin) { const t = await api.get('/teachers'); setTeachers(t.data) }
      if (isStudent) { const e = await api.get('/enrollments/me'); setEnrollments(e.data) }
    } catch (e) { setError(e.response?.data?.message || 'Could not load dashboard data.') }
  }
  useEffect(() => { loadAll() }, [user?.id])
  async function signIn(e) {
    e.preventDefault(); setError(''); setBusy(true)
    try {
      const {data} = await api.post('/auth/login',login)
      localStorage.setItem('college_token',data.token)
      localStorage.setItem('college_user',JSON.stringify(data.user))
      setUser(data.user); setTab('dashboard')
    } catch (e) { setError(e.response?.data?.message || 'Login failed. Check your email and password.') }
    finally { setBusy(false) }
  }
  function signOut() {
    localStorage.removeItem('college_token'); localStorage.removeItem('college_user')
    setUser(null); setCourses([]); setStudents([]); setTeachers([]); setEnrollments([])
  }
  async function act(fn, success) {
    setError(''); setNotice('')
    try { await fn(); setNotice(success); await loadAll() }
    catch (e) { setError(e.response?.data?.message || e.response?.data?.error || 'Operation failed.') }
  }
  async function saveCourse(e) {
    e.preventDefault()
    await act(() => editingCourse ? api.put(`/courses/${editingCourse}`,courseForm) : api.post('/courses',courseForm),
      editingCourse ? 'Course updated.' : 'Course added.')
    setCourseForm(emptyCourse); setEditingCourse(null)
  }
  async function saveStudent(e) {
    e.preventDefault()
    await act(() => editingStudent ? api.put(`/students/${editingStudent}`,studentForm) : api.post('/students',studentForm),
      editingStudent ? 'Student updated.' : 'Student added.')
    setStudentForm(emptyStudent); setEditingStudent(null)
  }
  async function saveTeacher(e) {
    e.preventDefault()
    await act(() => api.post('/admin/teachers',teacherForm),'Teacher account created.')
    setTeacherForm({name:'',email:'',password:'',department:'CSE',phone:''})
  }
  async function openCourseStudents(course) {
    setSelectedCourse(course)
    await act(async () => { const r = await api.get(`/courses/${course.id}/enrollments`); setEnrollments(r.data) },'')
    setTab('courseStudents')
  }
  const filteredStudents = students.filter(s => `${s.name} ${s.email} ${s.rollNumber}`.toLowerCase().includes(search.toLowerCase()))
  const filteredCourses = courses.filter(c => `${c.title} ${c.code} ${c.teacherName}`.toLowerCase().includes(search.toLowerCase()))

  if (!user) return <div className="login-wrap"><div className="login-card"><div className="brand-mark"><GraduationCap size={30}/></div><p className="eyebrow">COLLEGE PORTAL</p><h1>Welcome back</h1><p className="muted">Sign in to manage courses, students and enrollments.</p>
    <form onSubmit={signIn} className="stack"><label>Email<input type="email" required value={login.email} onChange={e=>setLogin({...login,email:e.target.value})} placeholder="you@college.com"/></label><label>Password<input type="password" required value={login.password} onChange={e=>setLogin({...login,password:e.target.value})} placeholder="Enter password"/></label>{error && <div className="alert error">{error}</div>}<button className="primary full" disabled={busy}>{busy?'Signing in...':'Sign in'}</button></form>
    <div className="demo-box"><b>Demo accounts</b><span>Admin: admin@college.com / Admin@123</span><span>Teacher: teacher@college.com / Teacher@123</span><span>Student: student@college.com / Student@123</span></div>
  </div></div>

  const nav = [{id:'dashboard',label:'Dashboard',icon:LayoutDashboard},...(isAdmin||isTeacher?[{id:'students',label:'Students',icon:Users}]:[]),{id:'courses',label:'Courses',icon:BookOpen},...(isAdmin?[{id:'teachers',label:'Teachers',icon:GraduationCap}]:[]),...(isStudent?[{id:'myCourses',label:'My enrollments',icon:CheckCircle2}]:[])]
  return <div className="app-shell"><aside className="sidebar"><div className="side-brand"><div className="brand-mark"><GraduationCap/></div><div><b>CampusFlow</b><small>Management portal</small></div></div><div className="side-label">WORKSPACE</div><nav>{nav.map(n=><button key={n.id} className={tab===n.id?'nav-item active':'nav-item'} onClick={()=>{setTab(n.id);setSearch('')}}><n.icon size={18}/>{n.label}</button>)}</nav><div className="side-bottom"><div className="profile"><div className="avatar">{user.name?.[0]}</div><div><b>{user.name}</b><small>{role}</small></div></div><button className="logout" onClick={signOut}><LogOut size={17}/> Sign out</button></div></aside>
  <main className="main"><header className="topbar"><div><span className="eyebrow">COLLEGE MANAGEMENT</span><h2>{tab==='dashboard'?'Overview':tab==='myCourses'?'My enrollments':tab==='courseStudents'?'Enrolled students':tab[0].toUpperCase()+tab.slice(1)}</h2></div><div className="top-user"><span className="status-dot"></span> Signed in as {role?.toLowerCase()}</div></header>
  <section className="content">{error && <div className="alert error">{error}<button onClick={()=>setError('')}><X size={15}/></button></div>}{notice && <div className="alert success">{notice}<button onClick={()=>setNotice('')}><X size={15}/></button></div>}
  {tab==='dashboard' && <><div className="welcome"><div><p className="eyebrow">YOUR WORKSPACE</p><h1>Hello, {user.name.split(' ')[0]} 👋</h1><p className="muted">{isAdmin?'Here is what is happening across your college.':isTeacher?'Track your courses and student participation.':'Explore courses and manage your learning.'}</p></div></div>
    <div className="stats">{(isAdmin?[['Students',summary.students,Users],['Teachers',summary.teachers,GraduationCap],['Courses',summary.courses,BookOpen],['Enrollments',summary.enrollments,CheckCircle2]]:isTeacher?[['Your courses',summary.courses,BookOpen],['Total enrollments',summary.students,Users]]:[['Available courses',summary.availableCourses,BookOpen],['My enrollments',summary.myEnrollments,CheckCircle2]]).map(([label,value,Icon])=><div className="stat-card" key={label}><div className="stat-icon"><Icon size={19}/></div><span>{label}</span><strong>{value??0}</strong></div>)}</div>
    {isTeacher && <div className="panel"><div className="panel-head"><div><h3>Enrollment by subject</h3><p className="muted">Number of students enrolled in each of your courses</p></div></div><div className="table-wrap"><table><thead><tr><th>Course</th><th>Code</th><th>Students enrolled</th></tr></thead><tbody>{(summary.courseCounts||[]).map(x=><tr key={x.code}><td>{x.course}</td><td>{x.code}</td><td><b>{x.enrolled}</b></td></tr>)}</tbody></table></div></div>}
    {isAdmin && <div className="quick-grid"><button onClick={()=>setTab('students')}><Users/> Manage students <span>→</span></button><button onClick={()=>setTab('courses')}><BookOpen/> Manage courses <span>→</span></button><button onClick={()=>setTab('teachers')}><GraduationCap/> Manage teachers <span>→</span></button></div>}
  </>}
  {tab==='courses' && <><div className="toolbar"><div className="search"><Search size={17}/><input value={search} onChange={e=>setSearch(e.target.value)} placeholder="Search courses..."/></div>{(isTeacher||isAdmin)&&<button className="primary" onClick={()=>{setEditingCourse(null);setCourseForm(emptyCourse)}}><Plus size={17}/> Add course</button>}</div>
    {(isTeacher||isAdmin)&&<form className="panel form-panel" onSubmit={saveCourse}><div className="panel-head"><h3>{editingCourse?'Edit course':'Add a course'}</h3>{editingCourse&&<button type="button" className="icon-btn" onClick={()=>{setEditingCourse(null);setCourseForm(emptyCourse)}}><X/></button>}</div><div className="form-grid"><label>Course title<input required value={courseForm.title} onChange={e=>setCourseForm({...courseForm,title:e.target.value})}/></label><label>Course code<input required value={courseForm.code} onChange={e=>setCourseForm({...courseForm,code:e.target.value.toUpperCase()})}/></label><label>Credits<input type="number" min="1" max="10" value={courseForm.credits} onChange={e=>setCourseForm({...courseForm,credits:e.target.value})}/></label><label>Capacity<input type="number" min="1" value={courseForm.capacity} onChange={e=>setCourseForm({...courseForm,capacity:e.target.value})}/></label><label className="wide">Description<textarea value={courseForm.description} onChange={e=>setCourseForm({...courseForm,description:e.target.value})}/></label></div><button className="primary">{editingCourse?'Save changes':'Create course'}</button></form>}
    <div className="course-grid">{filteredCourses.map(c=><div className="course-card" key={c.id}><div className="course-top"><span className="course-code">{c.code}</span><span className="credits">{c.credits} credits</span></div><h3>{c.title}</h3><p className="muted">{c.description||'No course description provided.'}</p><div className="course-meta"><span><Users size={15}/> {c.enrolledCount} / {c.capacity}</span><span>{c.teacherName}</span></div><div className="course-actions">{isStudent?<button className="primary small" onClick={()=>act(()=>api.post(`/enrollments/${c.id}`),'Successfully enrolled.')}>Enroll</button>:<><button className="text-btn" onClick={()=>openCourseStudents(c)}>View students</button><button className="icon-btn" title="Edit" onClick={()=>{setEditingCourse(c.id);setCourseForm({title:c.title,code:c.code,description:c.description||'',credits:c.credits,capacity:c.capacity})}}><Pencil size={16}/></button><button className="icon-btn danger" title="Delete" onClick={()=>confirm('Delete this course and its enrollments?')&&act(()=>api.delete(`/courses/${c.id}`),'Course deleted.')}><Trash2 size={16}/></button></>}</div></div>)}</div>
  </>}
  {tab==='students' && <><div className="toolbar"><div className="search"><Search size={17}/><input value={search} onChange={e=>setSearch(e.target.value)} placeholder="Search students..."/></div></div><form className="panel form-panel" onSubmit={saveStudent}><div className="panel-head"><h3>{editingStudent?'Edit student':'Add student'}</h3>{editingStudent&&<button type="button" className="icon-btn" onClick={()=>{setEditingStudent(null);setStudentForm(emptyStudent)}}><X/></button>}</div><div className="form-grid"><label>Full name<input required value={studentForm.name} onChange={e=>setStudentForm({...studentForm,name:e.target.value})}/></label><label>Email<input type="email" required value={studentForm.email} onChange={e=>setStudentForm({...studentForm,email:e.target.value})}/></label><label>Roll number<input value={studentForm.rollNumber} onChange={e=>setStudentForm({...studentForm,rollNumber:e.target.value})}/></label><label>Department<input value={studentForm.department} onChange={e=>setStudentForm({...studentForm,department:e.target.value})}/></label><label>Phone<input value={studentForm.phone} onChange={e=>setStudentForm({...studentForm,phone:e.target.value})}/></label><label>Password {editingStudent?'(leave blank to keep current)':'(default Student@123)'}<input type="password" value={studentForm.password} onChange={e=>setStudentForm({...studentForm,password:e.target.value})}/></label></div><button className="primary">{editingStudent?'Update student':'Add student'}</button></form><div className="panel"><div className="panel-head"><div><h3>Student directory</h3><p className="muted">{filteredStudents.length} students</p></div></div><div className="table-wrap"><table><thead><tr><th>Name</th><th>Roll number</th><th>Email</th><th>Department</th><th>Actions</th></tr></thead><tbody>{filteredStudents.map(s=><tr key={s.id}><td><b>{s.name}</b></td><td>{s.rollNumber}</td><td>{s.email}</td><td>{s.department}</td><td className="actions"><button className="icon-btn" onClick={()=>{setEditingStudent(s.id);setStudentForm({...emptyStudent,...s,password:''});window.scrollTo({top:0,behavior:'smooth'})}}><Pencil size={16}/></button><button className="icon-btn danger" onClick={()=>confirm(`Delete ${s.name}? Their enrollments will also be deleted.`)&&act(()=>api.delete(`/students/${s.id}`),'Student deleted.')}><Trash2 size={16}/></button></td></tr>)}</tbody></table></div></div></>}
  {tab==='teachers' && <><form className="panel form-panel" onSubmit={saveTeacher}><div className="panel-head"><h3>Add teacher account</h3></div><div className="form-grid"><label>Full name<input required value={teacherForm.name} onChange={e=>setTeacherForm({...teacherForm,name:e.target.value})}/></label><label>Email<input type="email" required value={teacherForm.email} onChange={e=>setTeacherForm({...teacherForm,email:e.target.value})}/></label><label>Password<input type="password" required value={teacherForm.password} onChange={e=>setTeacherForm({...teacherForm,password:e.target.value})}/></label><label>Department<input value={teacherForm.department} onChange={e=>setTeacherForm({...teacherForm,department:e.target.value})}/></label><label>Phone<input value={teacherForm.phone} onChange={e=>setTeacherForm({...teacherForm,phone:e.target.value})}/></label></div><button className="primary">Create teacher</button></form><div className="panel"><div className="panel-head"><h3>Teacher directory</h3></div><div className="table-wrap"><table><thead><tr><th>Name</th><th>Email</th><th>Department</th><th>Action</th></tr></thead><tbody>{teachers.map(t=><tr key={t.id}><td><b>{t.name}</b></td><td>{t.email}</td><td>{t.department}</td><td><button className="icon-btn danger" onClick={()=>confirm(`Delete ${t.name} and all courses owned by them?`)&&act(()=>api.delete(`/admin/teachers/${t.id}`),'Teacher deleted.')}><Trash2 size={16}/></button></td></tr>)}</tbody></table></div></div></>}
  {tab==='myCourses' && <div className="panel"><div className="panel-head"><div><h3>Your enrolled courses</h3><p className="muted">Courses you have joined</p></div></div><div className="table-wrap"><table><thead><tr><th>Course</th><th>Code</th><th>Teacher</th><th>Action</th></tr></thead><tbody>{enrollments.map(e=><tr key={e.id}><td><b>{e.course.title}</b></td><td>{e.course.code}</td><td>{e.course.teacherName}</td><td><button className="text-btn danger-text" onClick={()=>confirm('Cancel this enrollment?')&&act(()=>api.delete(`/enrollments/${e.course.id}`),'Enrollment cancelled.')}>Cancel</button></td></tr>)}</tbody></table></div></div>}
  {tab==='courseStudents' && <div className="panel"><div className="panel-head"><div><h3>{selectedCourse?.title}</h3><p className="muted">Students enrolled in {selectedCourse?.code}</p></div><button className="secondary" onClick={()=>setTab('courses')}>Back to courses</button></div><div className="table-wrap"><table><thead><tr><th>Student</th><th>Roll number</th><th>Email</th><th>Department</th></tr></thead><tbody>{enrollments.map(e=><tr key={e.id}><td><b>{e.student.name}</b></td><td>{e.student.rollNumber}</td><td>{e.student.email}</td><td>{e.student.department}</td></tr>)}</tbody></table></div><p className="muted bottom-note">Total enrolled: {enrollments.length}</p></div>}
  </section></main></div>
}
